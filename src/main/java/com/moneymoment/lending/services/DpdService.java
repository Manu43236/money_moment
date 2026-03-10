package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.dtos.EodResultDto;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.master.entities.LoanStatusesEntity;
import com.moneymoment.lending.master.repos.LoanStatusesRepo;
import com.moneymoment.lending.repos.EmiScheduleRepository;
import com.moneymoment.lending.repos.LoanRepo;

@Service
public class DpdService {

    private LoanStatusesRepo loanStatusesRepo;
    private EmiScheduleRepository emiScheduleRepository;
    private LoanRepo loanRepo;

    public DpdService(
            EmiScheduleRepository emiScheduleRepository,
            LoanStatusesRepo loanStatusesRepo,
            LoanRepo loanRepo) {

        this.emiScheduleRepository = emiScheduleRepository;
        this.loanStatusesRepo = loanStatusesRepo;
        this.loanRepo = loanRepo;
    }

    @Transactional
    public EmiScheduleEntity calculateDpdForEmi(Long emiScheduleId) {

        // 1. Fetch EMI
        EmiScheduleEntity emi = emiScheduleRepository.findById(emiScheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("EMI", "id", emiScheduleId));

        // 2. Check if EMI is already PAID
        if (emi.getStatus().equals("PAID")) {
            emi.setDaysPastDue(0);
            return emiScheduleRepository.save(emi);
        }

        // 3. Calculate DPD
        LocalDate today = LocalDate.now();
        LocalDate dueDate = emi.getDueDate();

        int dpd = 0;
        if (today.isAfter(dueDate)) {
            dpd = (int) ChronoUnit.DAYS.between(dueDate, today);
        }

        // 4. Update EMI
        emi.setDaysPastDue(dpd);

        // 5. Update status based on DPD
        if (dpd > 0 && !emi.getStatus().equals("PAID")) {
            emi.setStatus("OVERDUE");
        }

        return emiScheduleRepository.save(emi);
    }

    @Transactional
    public void calculateDpdForLoan(Long loanId) {
        // 1. Fetch all EMIs for loan
        List<EmiScheduleEntity> emis = emiScheduleRepository.findByLoanIdOrderByEmiNumberAsc(loanId);

        // 2. Calculate DPD for each EMI
        for (EmiScheduleEntity emi : emis) {
            calculateDpdForEmi(emi.getId());
        }

        // 3. Update loan fields
        updateLoanStatus(loanId);
    }

    // Called by EOD — runs in its own transaction so a failure doesn't poison the outer EOD transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String updateLoanStatusForEod(Long loanId) {
        return updateLoanStatus(loanId);
    }

    // Called by processPayment — joins the caller's transaction (must share lock on loan row)
    @Transactional
    public String updateLoanStatus(Long loanId) {
        // 1. Fetch loan
        LoanEntity loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", loanId));

        // 2. Get all EMIs
        List<EmiScheduleEntity> emis = emiScheduleRepository.findByLoanIdOrderByEmiNumberAsc(loanId);

        // 3. Find highest DPD
        int highestDpd = emis.stream()
                .mapToInt(emi -> emi.getDaysPastDue() != null ? emi.getDaysPastDue() : 0)
                .max()
                .orElse(0);

        // 4. Count overdue EMIs
        long overdueCount = emis.stream()
                .filter(emi -> emi.getStatus().equals("OVERDUE"))
                .count();

        // 5. Calculate total overdue amount
        Double totalOverdue = emis.stream()
                .filter(emi -> emi.getStatus().equals("OVERDUE"))
                .mapToDouble(emi -> emi.getEmiAmount() - (emi.getAmountPaid() != null ? emi.getAmountPaid() : 0.0))
                .sum();

        // 6. Update loan fields
        loan.setCurrentDpd(highestDpd);
        int currentHighest = loan.getHighestDpd() != null ? loan.getHighestDpd() : 0;
        if (highestDpd > currentHighest) {
            loan.setHighestDpd(highestDpd);
        }
        loan.setNumberOfOverdueEmis((int) overdueCount);
        loan.setTotalOverdueAmount(totalOverdue);

        // 7. Determine loan status — skip if loan is already CLOSED
        String newStatusCode = null;
        String currentCode = loan.getLoanStatus().getCode();
        if (!currentCode.equals("CLOSED")) {
            if (highestDpd == 0) {
                int paidEmis = loan.getNumberOfPaidEmis() != null ? loan.getNumberOfPaidEmis() : 0;
                if ("NPA".equals(currentCode)) {
                    // Fix 4: NPA upgrade requires 3 consecutive payments after last overdue
                    int recoveryCount = loan.getNpaRecoveryPaymentCount() != null ? loan.getNpaRecoveryPaymentCount() : 0;
                    newStatusCode = recoveryCount >= 3 ? "ACTIVE" : "NPA";
                } else {
                    newStatusCode = paidEmis > 0 ? "ACTIVE" : "DISBURSED";
                }
            } else if (highestDpd < 90) {
                newStatusCode = "OVERDUE";
            } else {
                newStatusCode = "NPA";
            }

            LoanStatusesEntity newStatus = loanStatusesRepo.findByCode(newStatusCode).orElse(null);
            if (newStatus != null) {
                loan.setLoanStatus(newStatus);
            }
        }

        loanRepo.save(loan);
        return newStatusCode;
    }

    /**
     * Optimized batch EOD processing:
     * Before: ~19,000 individual DB calls (9000 findById + 9000 save per EMI + 271×3 for loans)
     * After:  4 DB calls total (2 batch loads + 2 batch saves)
     */
    @Transactional
    public EodResultDto processAllOverdueEmis() {
        EodResultDto result = new EodResultDto();
        LocalDate today = LocalDate.now();

        List<String> activeCodes = List.of("DISBURSED", "CURRENT", "ACTIVE", "OVERDUE", "NPA");

        // 1. Load all active loans in ONE query (JOIN FETCH loanStatus)
        List<LoanEntity> loans = loanRepo.findByLoanStatusCodes(activeCodes);
        result.setTotalLoansProcessed(loans.size());

        // 2. Load ALL their EMIs in ONE query (JOIN FETCH loan + loanStatus)
        List<EmiScheduleEntity> allEmis = emiScheduleRepository.findEmisByLoanStatusCodes(activeCodes);
        result.setTotalEmisProcessed(allEmis.size());

        // 3. Calculate DPD for every EMI in memory — zero extra DB calls
        for (EmiScheduleEntity emi : allEmis) {
            if (emi.getStatus().equals("PAID")) {
                emi.setDaysPastDue(0);
                result.setEmisAlreadyPaid(result.getEmisAlreadyPaid() + 1);
            } else {
                int dpd = 0;
                if (emi.getDueDate() != null && today.isAfter(emi.getDueDate())) {
                    dpd = (int) ChronoUnit.DAYS.between(emi.getDueDate(), today);
                }
                emi.setDaysPastDue(dpd);
                if (dpd > 0) {
                    emi.setStatus("OVERDUE");
                    result.setEmisMarkedOverdue(result.getEmisMarkedOverdue() + 1);
                }
            }
        }

        // 4. Batch save all EMIs in ONE saveAll call
        emiScheduleRepository.saveAll(allEmis);

        // 5. Pre-load all loan statuses once
        Map<String, LoanStatusesEntity> statusMap = new HashMap<>();
        for (String code : List.of("ACTIVE", "OVERDUE", "NPA", "DISBURSED")) {
            loanStatusesRepo.findByCode(code).ifPresent(s -> statusMap.put(code, s));
        }

        // 6. Group EMIs by loan ID (already in memory)
        Map<Long, List<EmiScheduleEntity>> emisByLoan = allEmis.stream()
                .collect(Collectors.groupingBy(emi -> emi.getLoan().getId()));

        // 7. Calculate loan status in memory — zero extra DB calls
        for (LoanEntity loan : loans) {
            List<EmiScheduleEntity> loanEmis = emisByLoan.getOrDefault(loan.getId(), List.of());

            int highestDpd = loanEmis.stream()
                    .mapToInt(emi -> emi.getDaysPastDue() != null ? emi.getDaysPastDue() : 0)
                    .max().orElse(0);

            long overdueCount = loanEmis.stream()
                    .filter(emi -> "OVERDUE".equals(emi.getStatus())).count();

            double totalOverdue = loanEmis.stream()
                    .filter(emi -> "OVERDUE".equals(emi.getStatus()))
                    .mapToDouble(emi -> emi.getEmiAmount() - (emi.getAmountPaid() != null ? emi.getAmountPaid() : 0.0))
                    .sum();

            loan.setCurrentDpd(highestDpd);
            if (loan.getHighestDpd() == null || highestDpd > loan.getHighestDpd()) {
                loan.setHighestDpd(highestDpd);
            }

            // Fix 4: Reset NPA recovery counter if a new EMI just became overdue this cycle
            int previousOverdueCount = loan.getNumberOfOverdueEmis() != null ? loan.getNumberOfOverdueEmis() : 0;
            if ((int) overdueCount > previousOverdueCount) {
                loan.setNpaRecoveryPaymentCount(0);
            }

            loan.setNumberOfOverdueEmis((int) overdueCount);
            loan.setTotalOverdueAmount(totalOverdue);

            String currentCode = loan.getLoanStatus().getCode();
            if (!currentCode.equals("CLOSED")) {
                String newStatusCode;
                if (highestDpd == 0) {
                    int paidEmis = loan.getNumberOfPaidEmis() != null ? loan.getNumberOfPaidEmis() : 0;
                    if ("NPA".equals(currentCode)) {
                        // Fix 4: NPA upgrade requires 3 consecutive payments after last overdue
                        int recoveryCount = loan.getNpaRecoveryPaymentCount() != null ? loan.getNpaRecoveryPaymentCount() : 0;
                        newStatusCode = recoveryCount >= 3 ? "ACTIVE" : "NPA";
                    } else {
                        newStatusCode = paidEmis > 0 ? "ACTIVE" : "DISBURSED";
                    }
                } else if (highestDpd < 90) {
                    newStatusCode = "OVERDUE";
                } else {
                    newStatusCode = "NPA";
                }

                LoanStatusesEntity newStatus = statusMap.get(newStatusCode);
                if (newStatus != null) loan.setLoanStatus(newStatus);

                if ("ACTIVE".equals(newStatusCode))    result.setLoansMarkedActive(result.getLoansMarkedActive() + 1);
                else if ("OVERDUE".equals(newStatusCode)) result.setLoansMarkedOverdue(result.getLoansMarkedOverdue() + 1);
                else if ("NPA".equals(newStatusCode))  result.setLoansMarkedNpa(result.getLoansMarkedNpa() + 1);
                else if ("DISBURSED".equals(newStatusCode)) result.setLoansStayedDisbursed(result.getLoansStayedDisbursed() + 1);
            }
        }

        // 8. Batch save all loans in ONE saveAll call
        loanRepo.saveAll(loans);

        return result;
    }
}
