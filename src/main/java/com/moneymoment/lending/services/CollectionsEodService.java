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
public class CollectionsEodService {

    private final LoanRepo loanRepo;

    public CollectionsEodService(LoanRepo loanRepo) {
        this.loanRepo = loanRepo;
    }

    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        List<LoanEntity> overdueLoans = loanRepo.findAll().stream()
                .filter(loan -> {
                    String code = loan.getLoanStatus().getCode();
                    return code.equals("OVERDUE") || code.equals("NPA");
                })
                .toList();

        // Bucket by DPD
        long bucket1to30   = overdueLoans.stream().filter(l -> l.getCurrentDpd() != null && l.getCurrentDpd() >= 1  && l.getCurrentDpd() <= 30).count();
        long bucket31to60  = overdueLoans.stream().filter(l -> l.getCurrentDpd() != null && l.getCurrentDpd() >= 31 && l.getCurrentDpd() <= 60).count();
        long bucket61to90  = overdueLoans.stream().filter(l -> l.getCurrentDpd() != null && l.getCurrentDpd() >= 61 && l.getCurrentDpd() <= 90).count();
        long bucketAbove90 = overdueLoans.stream().filter(l -> l.getCurrentDpd() != null && l.getCurrentDpd() > 90).count();

        // Mock: generate collection list and alert customers
        int customersAlerted = overdueLoans.size();
        double totalOverdueAmount = overdueLoans.stream()
                .mapToDouble(l -> l.getTotalOverdueAmount() != null ? l.getTotalOverdueAmount() : 0.0)
                .sum();

        // Log simulated alerts
        for (LoanEntity loan : overdueLoans) {
            log.debug("ALERT [SMS+EMAIL]: Customer {} | Loan {} | DPD {} | Overdue ₹{}",
                    loan.getCustomer().getName(),
                    loan.getLoanNumber(),
                    loan.getCurrentDpd(),
                    loan.getTotalOverdueAmount());
        }

        metrics.put("totalOverdueLoans", overdueLoans.size());
        metrics.put("bucket_1_30_days", bucket1to30);
        metrics.put("bucket_31_60_days", bucket31to60);
        metrics.put("bucket_61_90_days", bucket61to90);
        metrics.put("bucket_above_90_days", bucketAbove90);
        metrics.put("customersAlerted", customersAlerted);
        metrics.put("totalOverdueAmount", Math.round(totalOverdueAmount * 100.0) / 100.0);

        log.info("Collections: {} overdue loans | DPD buckets: 1-30={}, 31-60={}, 61-90={}, 90+={} | {} alerts sent",
                overdueLoans.size(), bucket1to30, bucket31to60, bucket61to90, bucketAbove90, customersAlerted);
        return metrics;
    }
}
