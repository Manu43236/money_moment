package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegulatoryService {

    private final LoanRepo loanRepo;

    public RegulatoryService(LoanRepo loanRepo) {
        this.loanRepo = loanRepo;
    }

    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        List<LoanEntity> allLoans = loanRepo.findAll();

        long npaCount = allLoans.stream()
                .filter(l -> l.getLoanStatus().getCode().equals("NPA")).count();
        long overdueCount = allLoans.stream()
                .filter(l -> l.getLoanStatus().getCode().equals("OVERDUE")).count();
        long activeCount = allLoans.stream()
                .filter(l -> l.getLoanStatus().getCode().equals("ACTIVE")).count();

        double totalOutstanding = allLoans.stream()
                .filter(l -> !l.getLoanStatus().getCode().equals("CLOSED"))
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0.0)
                .sum();

        double npaOutstanding = allLoans.stream()
                .filter(l -> l.getLoanStatus().getCode().equals("NPA"))
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0.0)
                .sum();

        double npaRatio = totalOutstanding > 0
                ? Math.round((npaOutstanding / totalOutstanding) * 10000.0) / 100.0 : 0.0;

        // Mock RBI NPA Report
        metrics.put("reportDate", LocalDate.now().toString());
        metrics.put("totalLoans", allLoans.size());
        metrics.put("npaLoans", npaCount);
        metrics.put("overdueLoans", overdueCount);
        metrics.put("activeLoans", activeCount);
        metrics.put("totalOutstanding", Math.round(totalOutstanding * 100.0) / 100.0);
        metrics.put("npaOutstanding", Math.round(npaOutstanding * 100.0) / 100.0);
        metrics.put("npaRatioPct", npaRatio);
        metrics.put("rbiReportStatus", "GENERATED");

        // Mock CIBIL bureau upload
        long cibilRecords = allLoans.stream()
                .filter(l -> !l.getLoanStatus().getCode().equals("INITIATED")).count();
        metrics.put("cibilRecordsQueued", cibilRecords);
        metrics.put("cibilUploadStatus", "QUEUED");

        log.info("Regulatory: NPA Ratio={}%, NPA Loans={}, CIBIL Records Queued={}", npaRatio, npaCount, cibilRecords);
        return metrics;
    }
}
