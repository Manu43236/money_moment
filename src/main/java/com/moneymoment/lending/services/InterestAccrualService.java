package com.moneymoment.lending.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InterestAccrualService {

    private final LoanRepo loanRepo;

    public InterestAccrualService(LoanRepo loanRepo) {
        this.loanRepo = loanRepo;
    }

    @Transactional
    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        List<LoanEntity> activeLoans = loanRepo.findAll().stream()
                .filter(loan -> {
                    String code = loan.getLoanStatus().getCode();
                    return code.equals("ACTIVE") || code.equals("OVERDUE") || code.equals("NPA");
                })
                .toList();

        double totalAccrued = 0.0;
        int loansProcessed = 0;

        for (LoanEntity loan : activeLoans) {
            try {
                double outstanding = loan.getOutstandingAmount() != null ? loan.getOutstandingAmount() : 0.0;
                double annualRate = loan.getInterestRate() != null ? loan.getInterestRate() : 0.0;

                // Daily interest = outstandingPrincipal × annualRate / 365
                double dailyInterest = Math.round((outstanding * annualRate / 100.0 / 365.0) * 100.0) / 100.0;

                double existing = loan.getAccruedInterest() != null ? loan.getAccruedInterest() : 0.0;
                loan.setAccruedInterest(Math.round((existing + dailyInterest) * 100.0) / 100.0);

                loanRepo.save(loan);
                totalAccrued += dailyInterest;
                loansProcessed++;
            } catch (Exception e) {
                log.error("Interest accrual failed for loan {}: {}", loan.getLoanNumber(), e.getMessage());
            }
        }

        metrics.put("loansProcessed", loansProcessed);
        metrics.put("totalDailyInterestAccrued", Math.round(totalAccrued * 100.0) / 100.0);
        log.info("Interest Accrual: {} loans processed, ₹{} accrued today", loansProcessed, totalAccrued);
        return metrics;
    }
}
