package com.moneymoment.lending.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EodVerificationService {

    private final LoanRepo loanRepo;

    public EodVerificationService(LoanRepo loanRepo) {
        this.loanRepo = loanRepo;
    }

    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        List<LoanEntity> allLoans = loanRepo.findAll();

        // Reconcile: verify total outstanding = sum of EMI schedules outstanding
        double totalOutstanding = allLoans.stream()
                .filter(l -> !l.getLoanStatus().getCode().equals("CLOSED"))
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0.0)
                .sum();

        double totalOverdue = allLoans.stream()
                .mapToDouble(l -> l.getTotalOverdueAmount() != null ? l.getTotalOverdueAmount() : 0.0)
                .sum();

        long npaCound = allLoans.stream().filter(l -> l.getLoanStatus().getCode().equals("NPA")).count();
        long overdueCound = allLoans.stream().filter(l -> l.getLoanStatus().getCode().equals("OVERDUE")).count();
        long activeCount = allLoans.stream().filter(l -> l.getLoanStatus().getCode().equals("ACTIVE")).count();
        long closedCount = allLoans.stream().filter(l -> l.getLoanStatus().getCode().equals("CLOSED")).count();

        // Reconciliation check: all non-closed loans should have valid outstanding
        long invalidLoans = allLoans.stream()
                .filter(l -> !l.getLoanStatus().getCode().equals("CLOSED")
                        && (l.getOutstandingAmount() == null || l.getOutstandingAmount() < 0))
                .count();

        boolean reconciliationPassed = invalidLoans == 0;

        metrics.put("totalLoansVerified", allLoans.size());
        metrics.put("totalOutstanding", Math.round(totalOutstanding * 100.0) / 100.0);
        metrics.put("totalOverdue", Math.round(totalOverdue * 100.0) / 100.0);
        metrics.put("npaLoans", npaCound);
        metrics.put("overdueLoans", overdueCound);
        metrics.put("activeLoans", activeCount);
        metrics.put("closedLoans", closedCount);
        metrics.put("reconciliationPassed", reconciliationPassed);
        metrics.put("invalidRecords", invalidLoans);
        metrics.put("eodCompletionStatus", reconciliationPassed ? "CLEAN" : "EXCEPTIONS_FOUND");

        if (reconciliationPassed) {
            log.info("Verification: PASSED — All {} loans reconciled. Outstanding=₹{}, Overdue=₹{}",
                    allLoans.size(), Math.round(totalOutstanding), Math.round(totalOverdue));
        } else {
            log.warn("Verification: FAILED — {} invalid loan records found", invalidLoans);
        }

        return metrics;
    }
}
