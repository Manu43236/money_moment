package com.moneymoment.lending.crons;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moneymoment.lending.services.EodService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EodScheduler {

    private final EodService eodService;

    public EodScheduler(EodService eodService) {
        this.eodService = eodService;
    }

    // Runs every day at 11:59 PM IST
    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Kolkata")
    public void runDailyEod() {
        log.info("========== SCHEDULED EOD TRIGGERED ==========");
        eodService.processEod("SCHEDULER");
    }
}