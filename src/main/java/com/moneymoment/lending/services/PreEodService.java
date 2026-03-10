package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.repos.EodLogRepository;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PreEodService {

    private final EodLogRepository eodLogRepository;
    private final LoanRepo loanRepo;

    public PreEodService(EodLogRepository eodLogRepository, LoanRepo loanRepo) {
        this.eodLogRepository = eodLogRepository;
        this.loanRepo = loanRepo;
    }

    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        // 1. DB health check
        long loanCount = loanRepo.count();
        metrics.put("dbStatus", "OK");
        metrics.put("totalLoansInDb", loanCount);
        log.info("Pre-EOD: DB health OK, {} loans in system", loanCount);

        // 2. Check if EOD already ran today successfully
        LocalDate today = LocalDate.now();
        boolean ranToday = eodLogRepository.findAll().stream()
                .anyMatch(log -> log.getStatus().equals("SUCCESS")
                        && log.getRunDate() != null
                        && log.getRunDate().toLocalDate().equals(today));

        metrics.put("alreadyRanToday", ranToday);
        if (ranToday) {
            log.warn("Pre-EOD: EOD already ran successfully today — proceeding with re-run");
        }

        // 3. Record cutoff time
        metrics.put("cutoffTime", LocalDateTime.now().toString());
        metrics.put("businessDate", today.toString());

        log.info("Pre-EOD: Checks complete. CutoffTime={}, AlreadyRanToday={}", LocalDateTime.now(), ranToday);
        return metrics;
    }
}
