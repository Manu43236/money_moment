package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EodReportingService {

    private final LoanRepo loanRepo;

    public EodReportingService(LoanRepo loanRepo) {
        this.loanRepo = loanRepo;
    }

    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        List<LoanEntity> allLoans = loanRepo.findAll();

        // Portfolio MIS
        double totalDisbursed = allLoans.stream()
                .filter(l -> l.getLoanAmount() != null)
                .mapToDouble(LoanEntity::getLoanAmount).sum();

        double totalOutstanding = allLoans.stream()
                .filter(l -> l.getOutstandingAmount() != null && !l.getLoanStatus().getCode().equals("CLOSED"))
                .mapToDouble(LoanEntity::getOutstandingAmount).sum();

        double totalOverdue = allLoans.stream()
                .filter(l -> l.getTotalOverdueAmount() != null)
                .mapToDouble(LoanEntity::getTotalOverdueAmount).sum();

        double totalPenalties = allLoans.stream()
                .filter(l -> l.getTotalPenaltyAmount() != null)
                .mapToDouble(LoanEntity::getTotalPenaltyAmount).sum();

        // Branch-wise loan count
        Map<String, Long> branchWise = allLoans.stream()
                .filter(l -> l.getOriginatingBranchCode() != null)
                .collect(Collectors.groupingBy(LoanEntity::getOriginatingBranchCode, Collectors.counting()));

        // Status distribution
        Map<String, Long> statusDist = allLoans.stream()
                .collect(Collectors.groupingBy(l -> l.getLoanStatus().getCode(), Collectors.counting()));

        metrics.put("reportDate", LocalDate.now().toString());
        metrics.put("totalLoansInPortfolio", allLoans.size());
        metrics.put("totalDisbursedAmount", Math.round(totalDisbursed * 100.0) / 100.0);
        metrics.put("totalOutstandingAmount", Math.round(totalOutstanding * 100.0) / 100.0);
        metrics.put("totalOverdueAmount", Math.round(totalOverdue * 100.0) / 100.0);
        metrics.put("totalPenalties", Math.round(totalPenalties * 100.0) / 100.0);
        metrics.put("statusDistribution", statusDist);
        metrics.put("branchWiseLoans", branchWise);
        metrics.put("reportsGenerated", 3); // Portfolio MIS, Branch Report, Overdue Report

        log.info("Reports: Portfolio ₹{} disbursed, ₹{} outstanding, ₹{} overdue | {} branches",
                Math.round(totalDisbursed), Math.round(totalOutstanding), Math.round(totalOverdue), branchWise.size());
        return metrics;
    }
}
