package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
                newStatusCode = paidEmis > 0 ? "ACTIVE" : "DISBURSED";
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

    public EodResultDto processAllOverdueEmis() {
        EodResultDto result = new EodResultDto();

        // 1. Get all active lifecycle loans
        List<LoanEntity> loans = loanRepo.findAll().stream()
                .filter(loan -> {
                    String code = loan.getLoanStatus().getCode();
                    return code.equals("DISBURSED")
                            || code.equals("CURRENT")
                            || code.equals("ACTIVE")
                            || code.equals("OVERDUE")
                            || code.equals("NPA");
                })
                .toList();

        result.setTotalLoansProcessed(loans.size());

        // 2. Process each loan — collect stats
        for (LoanEntity loan : loans) {
            List<EmiScheduleEntity> emis = emiScheduleRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId());
            result.setTotalEmisProcessed(result.getTotalEmisProcessed() + emis.size());

            for (EmiScheduleEntity emi : emis) {
                if (emi.getStatus().equals("PAID")) {
                    result.setEmisAlreadyPaid(result.getEmisAlreadyPaid() + 1);
                }
                EmiScheduleEntity updated = calculateDpdForEmi(emi.getId());
                if (updated.getStatus().equals("OVERDUE")) {
                    result.setEmisMarkedOverdue(result.getEmisMarkedOverdue() + 1);
                }
            }

            // Update loan status and count
            String newStatus = updateLoanStatusForEod(loan.getId());
            if ("ACTIVE".equals(newStatus))    result.setLoansMarkedActive(result.getLoansMarkedActive() + 1);
            else if ("OVERDUE".equals(newStatus)) result.setLoansMarkedOverdue(result.getLoansMarkedOverdue() + 1);
            else if ("NPA".equals(newStatus))     result.setLoansMarkedNpa(result.getLoansMarkedNpa() + 1);
            else if ("DISBURSED".equals(newStatus)) result.setLoansStayedDisbursed(result.getLoansStayedDisbursed() + 1);
        }

        return result;
    }
}
