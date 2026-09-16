package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.dtos.PreClosureRequestDto;
import com.moneymoment.lending.entities.CollateralDetailsEntity;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.entities.LoanPenaltyEntity;
import com.moneymoment.lending.master.entities.LoanStatusesEntity;
import com.moneymoment.lending.master.entities.PreClosureConfigEntity;
import com.moneymoment.lending.master.repos.LoanStatusesRepo;
import com.moneymoment.lending.master.repos.PreClosureConfigRepository;
import com.moneymoment.lending.repos.CollateralDetailsRepository;
import com.moneymoment.lending.repos.EmiScheduleRepository;
import com.moneymoment.lending.repos.LoanPenaltyRepository;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
public class LoanClosureService {

    private final LoanRepo loanRepo;
    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanPenaltyRepository loanPenaltyRepository;
    private final CollateralDetailsRepository collateralDetailsRepository;
    private final LoanStatusesRepo loanStatusesRepo;
    private final PreClosureConfigRepository preClosureConfigRepository;

    public LoanClosureService(
            LoanRepo loanRepo,
            EmiScheduleRepository emiScheduleRepository,
            LoanPenaltyRepository loanPenaltyRepository,
            CollateralDetailsRepository collateralDetailsRepository,
            LoanStatusesRepo loanStatusesRepo,
            PreClosureConfigRepository preClosureConfigRepository) {
        this.loanRepo = loanRepo;
        this.emiScheduleRepository = emiScheduleRepository;
        this.loanPenaltyRepository = loanPenaltyRepository;
        this.collateralDetailsRepository = collateralDetailsRepository;
        this.loanStatusesRepo = loanStatusesRepo;
        this.preClosureConfigRepository = preClosureConfigRepository;
    }

    @Transactional
    public LoanClosureSummary closeLoan(String loanNumber) {

        // Step 1: Fetch loan
        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        // Step 2: Check if already closed
        if (loan.getLoanStatus().getCode().equals("CLOSED")) {
            throw new BusinessLogicException("Loan is already closed on " + loan.getClosedDate());
        }

        // Step 3: Validate all EMIs paid
        List<EmiScheduleEntity> allEmis = emiScheduleRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId());

        long unpaidEmis = allEmis.stream()
                .filter(emi -> !emi.getStatus().equals("PAID"))
                .count();

        if (unpaidEmis > 0) {
            throw new BusinessLogicException(
                    "Cannot close loan. " + unpaidEmis + " EMIs are still unpaid");
        }

        // Step 4: Check pending penalties
        List<LoanPenaltyEntity> penalties = loanPenaltyRepository.findByLoanIdAndIsPaid(loan.getId(), false);

        Double pendingPenalties = penalties.stream()
                .filter(p -> !p.getIsWaived())
                .mapToDouble(LoanPenaltyEntity::getPenaltyAmount)
                .sum();

        if (pendingPenalties > 0) {
            throw new BusinessLogicException(
                    "Cannot close loan. Pending penalties: ₹" + pendingPenalties +
                            ". Please pay or waive penalties first.");
        }

        // Step 5: Mark loan as CLOSED
        LoanStatusesEntity closedStatus = loanStatusesRepo.findByCode("CLOSED")
                .orElseThrow(() -> new ResourceNotFoundException("LoanStatus", "code", "CLOSED"));

        loan.setLoanStatus(closedStatus);
        loan.setClosedDate(LocalDateTime.now());
        loan.setOutstandingAmount(0.0);
        loan.setCurrentDpd(0);
        loan.setNumberOfOverdueEmis(0);
        loan.setTotalOverdueAmount(0.0);

        loan = loanRepo.save(loan);

        // Step 6: Release collateral (if secured loan)
        CollateralDetailsEntity collateral = null;
        if (loan.getLoanType().getCollateralRequired()) {
            collateral = collateralDetailsRepository.findByLoanId(loan.getId()).orElse(null);

            if (collateral != null && !collateral.getCollateralStatus().equals("RELEASED")) {
                collateral.setCollateralStatus("RELEASED");
                collateral.setReleaseDate(LocalDate.now());
                collateralDetailsRepository.save(collateral);
            }
        }

        // Step 7: Prepare closure summary
        return buildClosureSummary(loan, allEmis, collateral);
    }

    private LoanClosureSummary buildClosureSummary(
            LoanEntity loan,
            List<EmiScheduleEntity> emis,
            CollateralDetailsEntity collateral) {

        LoanClosureSummary summary = new LoanClosureSummary();

        // Loan details
        summary.setLoanNumber(loan.getLoanNumber());
        summary.setCustomerName(loan.getCustomer().getName());
        summary.setLoanAmount(loan.getLoanAmount());

        // Closure info
        summary.setClosedDate(loan.getClosedDate());
        summary.setTotalEmisPaid(loan.getNumberOfPaidEmis());

        // Financial summary
        summary.setTotalPrincipalPaid(loan.getLoanAmount());
        summary.setTotalInterestPaid(loan.getTotalInterest());
        summary.setTotalPenaltiesPaid(loan.getTotalPenaltyAmount() != null ? loan.getTotalPenaltyAmount() : 0.0);
        summary.setTotalAmountPaid(
                loan.getLoanAmount() +
                        loan.getTotalInterest() +
                        (loan.getTotalPenaltyAmount() != null ? loan.getTotalPenaltyAmount() : 0.0));

        // Dates
        summary.setDisbursedDate(loan.getDisbursedDate().toLocalDate());
        summary.setTenureMonths(loan.getTenureMonths());

        // Collateral
        if (collateral != null) {
            summary.setCollateralReleased(true);
            summary.setCollateralType(collateral.getCollateralType());
        } else {
            summary.setCollateralReleased(false);
        }

        return summary;
    }

    // ─── Pre-closure quote (read-only, no state change) ───────────────────────

    public PreClosureQuote getPreClosureQuote(String loanNumber) {
        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        if (loan.getLoanStatus().getCode().equals("CLOSED")) {
            throw new BusinessLogicException("Loan is already closed");
        }

        PreClosureConfigEntity config = preClosureConfigRepository
                .findByLoanType_CodeAndIsActive(loan.getLoanType().getCode(), true)
                .orElseThrow(() -> new BusinessLogicException(
                        "No pre-closure charge config found for loan type: " + loan.getLoanType().getCode()
                                + ". Please configure it in Admin → Masters → Pre-Closure Charges."));

        double outstanding = loan.getOutstandingAmount() != null ? loan.getOutstandingAmount() : 0.0;
        double charge = computeCharge(config, outstanding);

        double penalties = loanPenaltyRepository.findByLoanIdAndIsPaid(loan.getId(), false).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsWaived()))
                .mapToDouble(LoanPenaltyEntity::getPenaltyAmount)
                .sum();

        long remainingEmis = emiScheduleRepository.countByLoanIdAndStatus(loan.getId(), "PAID") == 0
                ? loan.getTenureMonths()
                : emiScheduleRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId())
                        .stream().filter(e -> !"PAID".equals(e.getStatus())).count();

        PreClosureQuote quote = new PreClosureQuote();
        quote.setLoanNumber(loan.getLoanNumber());
        quote.setCustomerName(loan.getCustomer().getName());
        quote.setLoanTypeName(loan.getLoanType().getName());
        quote.setOutstandingPrincipal(round(outstanding));
        quote.setPreClosureCharge(round(charge));
        quote.setChargeType(config.getChargeType());
        quote.setChargeValue(config.getChargeValue());
        quote.setPendingPenalties(round(penalties));
        quote.setTotalPayable(round(outstanding + charge + penalties));
        quote.setRemainingEmis((int) remainingEmis);
        return quote;
    }

    // ─── Pre-closure execution ─────────────────────────────────────────────────

    @Transactional
    public PreClosureSummaryFull preCloseLoan(String loanNumber, PreClosureRequestDto request) {
        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        if (loan.getLoanStatus().getCode().equals("CLOSED")) {
            throw new BusinessLogicException("Loan is already closed");
        }

        String loanTypeCode = loan.getLoanType().getCode();
        PreClosureConfigEntity config = preClosureConfigRepository
                .findByLoanType_CodeAndIsActive(loanTypeCode, true)
                .orElseThrow(() -> new BusinessLogicException(
                        "No pre-closure charge config found for loan type: " + loanTypeCode));

        double outstanding = loan.getOutstandingAmount() != null ? loan.getOutstandingAmount() : 0.0;
        double charge = computeCharge(config, outstanding);

        // Collect pending penalties
        List<LoanPenaltyEntity> pendingPenalties = loanPenaltyRepository
                .findByLoanIdAndIsPaid(loan.getId(), false).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsWaived()))
                .toList();
        double penaltiesTotal = pendingPenalties.stream()
                .mapToDouble(LoanPenaltyEntity::getPenaltyAmount).sum();

        // Mark all unpaid EMIs as PAID
        List<EmiScheduleEntity> allEmis = emiScheduleRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId());
        int emisCleared = 0;
        for (EmiScheduleEntity emi : allEmis) {
            if (!"PAID".equals(emi.getStatus())) {
                emi.setStatus("PAID");
                emi.setAmountPaid(emi.getEmiAmount());
                emi.setPaidDate(request.getPaymentDate());
                emi.setDaysPastDue(0);
                emiScheduleRepository.save(emi);
                emisCleared++;
            }
        }

        // Mark all pending penalties as PAID
        for (LoanPenaltyEntity penalty : pendingPenalties) {
            penalty.setIsPaid(true);
            penalty.setPaidAmount(penalty.getPenaltyAmount());
            penalty.setPaidDate(request.getPaymentDate());
            loanPenaltyRepository.save(penalty);
        }

        // Close the loan
        LoanStatusesEntity closedStatus = loanStatusesRepo.findByCode("CLOSED")
                .orElseThrow(() -> new ResourceNotFoundException("LoanStatus", "code", "CLOSED"));
        loan.setLoanStatus(closedStatus);
        loan.setClosedDate(LocalDateTime.now());
        loan.setOutstandingAmount(0.0);
        loan.setCurrentDpd(0);
        loan.setNumberOfOverdueEmis(0);
        loan.setTotalOverdueAmount(0.0);
        loan.setNumberOfPaidEmis(allEmis.size());
        loan = loanRepo.save(loan);

        // Release collateral
        CollateralDetailsEntity collateral = null;
        if (loan.getLoanType().getCollateralRequired()) {
            collateral = collateralDetailsRepository.findByLoanId(loan.getId()).orElse(null);
            if (collateral != null && !"RELEASED".equals(collateral.getCollateralStatus())) {
                collateral.setCollateralStatus("RELEASED");
                collateral.setReleaseDate(request.getPaymentDate());
                collateralDetailsRepository.save(collateral);
            }
        }

        PreClosureSummaryFull summary = new PreClosureSummaryFull();
        summary.setLoanNumber(loan.getLoanNumber());
        summary.setCustomerName(loan.getCustomer().getName());
        summary.setOutstandingPrincipal(round(outstanding));
        summary.setPreClosureCharge(round(charge));
        summary.setPendingPenalties(round(penaltiesTotal));
        summary.setTotalAmountPaid(round(outstanding + charge + penaltiesTotal));
        summary.setEmisCleared(emisCleared);
        summary.setClosedDate(loan.getClosedDate());
        summary.setCollateralReleased(collateral != null);
        summary.setCollateralType(collateral != null ? collateral.getCollateralType() : null);
        return summary;
    }

    private double computeCharge(PreClosureConfigEntity config, double outstanding) {
        double charge;
        if ("PERCENTAGE".equals(config.getChargeType())) {
            charge = (outstanding * config.getChargeValue()) / 100.0;
            if (config.getMinCharge() != null) charge = Math.max(charge, config.getMinCharge());
            if (config.getMaxCharge() != null) charge = Math.min(charge, config.getMaxCharge());
        } else {
            charge = config.getChargeValue();
        }
        return charge;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ─── Inner classes ─────────────────────────────────────────────────────────

    @Data
    public static class PreClosureQuote {
        private String loanNumber;
        private String customerName;
        private String loanTypeName;
        private Double outstandingPrincipal;
        private Double preClosureCharge;
        private String chargeType;
        private Double chargeValue;
        private Double pendingPenalties;
        private Double totalPayable;
        private Integer remainingEmis;
    }

    @Data
    public static class PreClosureSummaryFull {
        private String loanNumber;
        private String customerName;
        private Double outstandingPrincipal;
        private Double preClosureCharge;
        private Double pendingPenalties;
        private Double totalAmountPaid;
        private Integer emisCleared;
        private LocalDateTime closedDate;
        private Boolean collateralReleased;
        private String collateralType;
    }

    // Inner class for closure summary
    @Data
    @AllArgsConstructor
    public static class LoanClosureSummary {
        private String loanNumber;
        private String customerName;
        private Double loanAmount;
        private LocalDate disbursedDate;
        private LocalDateTime closedDate;
        private Integer tenureMonths;
        private Integer totalEmisPaid;
        private Double totalPrincipalPaid;
        private Double totalInterestPaid;
        private Double totalPenaltiesPaid;
        private Double totalAmountPaid;
        private Boolean collateralReleased;
        private String collateralType;

        public LoanClosureSummary() {
        }
    }
}