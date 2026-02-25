package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.entities.CollateralDetailsEntity;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.entities.LoanPenaltyEntity;
import com.moneymoment.lending.master.entities.LoanStatusesEntity;
import com.moneymoment.lending.master.repos.LoanStatusesRepo;
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

    public LoanClosureService(
            LoanRepo loanRepo,
            EmiScheduleRepository emiScheduleRepository,
            LoanPenaltyRepository loanPenaltyRepository,
            CollateralDetailsRepository collateralDetailsRepository,
            LoanStatusesRepo loanStatusesRepo) {
        this.loanRepo = loanRepo;
        this.emiScheduleRepository = emiScheduleRepository;
        this.loanPenaltyRepository = loanPenaltyRepository;
        this.collateralDetailsRepository = collateralDetailsRepository;
        this.loanStatusesRepo = loanStatusesRepo;
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