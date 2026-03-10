package com.moneymoment.lending.crons;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moneymoment.lending.services.EodAsyncExecutor;
import com.moneymoment.lending.services.EodService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EodScheduler {

    private final EodService eodService;
    private final EodAsyncExecutor eodAsyncExecutor;

    public EodScheduler(EodService eodService, EodAsyncExecutor eodAsyncExecutor) {
        this.eodService = eodService;
        this.eodAsyncExecutor = eodAsyncExecutor;
    }

    // Runs every day at 11:59 PM IST
    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Kolkata")
    public void runDailyEod() {
        log.info("========== SCHEDULED EOD TRIGGERED ==========");
        if (eodService.isRunning()) {
            log.warn("Scheduled EOD skipped — EOD is already running");
            return;
        }
        eodAsyncExecutor.run("SCHEDULER");
    }
}
