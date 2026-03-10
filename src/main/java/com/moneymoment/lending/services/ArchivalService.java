package com.moneymoment.lending.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ArchivalService {

    private final LoanRepo loanRepo;

    public ArchivalService(LoanRepo loanRepo) {
        this.loanRepo = loanRepo;
    }

    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        // Find CLOSED loans older than 90 days — eligible for archival
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        List<LoanEntity> archivableLoans = loanRepo.findAll().stream()
                .filter(l -> l.getLoanStatus().getCode().equals("CLOSED")
                        && l.getClosedDate() != null
                        && l.getClosedDate().isBefore(cutoff))
                .toList();

        // In a real system: move to archive_loans table / cold storage / S3
        // Here we identify and log
        metrics.put("archivableLoansIdentified", archivableLoans.size());
        metrics.put("archivalCutoffDate", cutoff.toLocalDate().toString());

        // Simulate purging temp/session data
        metrics.put("tempRecordsPurged", 0);
        metrics.put("archivalStatus", "IDENTIFIED"); // would be COMPLETED after actual archival

        if (!archivableLoans.isEmpty()) {
            log.info("Archival: {} CLOSED loans older than 90 days identified for archival", archivableLoans.size());
            archivableLoans.forEach(l -> log.debug("Archival candidate: Loan {} closed on {}", l.getLoanNumber(), l.getClosedDate()));
        } else {
            log.info("Archival: No loans eligible for archival today");
        }

        return metrics;
    }
}
