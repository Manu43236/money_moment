package com.moneymoment.lending.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NachProcessingService {

    private final LoanRepo loanRepo;
    private final Random random = new Random();

    public NachProcessingService(LoanRepo loanRepo) {
        this.loanRepo = loanRepo;
    }

    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        // Only ACTIVE loans are eligible for NACH mandate processing
        List<LoanEntity> eligibleLoans = loanRepo.findAll().stream()
                .filter(loan -> loan.getLoanStatus().getCode().equals("ACTIVE"))
                .toList();

        int mandatesPresented = eligibleLoans.size();
        int successCount = 0;
        int bounceCount = 0;
        double totalCollected = 0.0;

        for (LoanEntity loan : eligibleLoans) {
            // Simulate NACH processing: 85% success, 15% bounce
            boolean success = random.nextDouble() < 0.85;
            if (success) {
                successCount++;
                totalCollected += loan.getEmiAmount() != null ? loan.getEmiAmount() : 0.0;
                log.debug("NACH SUCCESS: Loan {} — ₹{}", loan.getLoanNumber(), loan.getEmiAmount());
            } else {
                bounceCount++;
                log.debug("NACH BOUNCE: Loan {} — mandate returned unpaid", loan.getLoanNumber());
            }
        }

        metrics.put("mandatesPresented", mandatesPresented);
        metrics.put("successCount", successCount);
        metrics.put("bounceCount", bounceCount);
        metrics.put("totalCollected", Math.round(totalCollected * 100.0) / 100.0);
        metrics.put("successRate", mandatesPresented > 0
                ? Math.round((successCount * 100.0 / mandatesPresented) * 10.0) / 10.0 : 0.0);

        log.info("NACH Processing: {} presented, {} success, {} bounced, ₹{} collected",
                mandatesPresented, successCount, bounceCount, totalCollected);
        return metrics;
    }
}
