package com.moneymoment.lending.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EodAsyncExecutor {

    private final EodService eodService;

    public EodAsyncExecutor(EodService eodService) {
        this.eodService = eodService;
    }

    @Async("eodExecutor")
    public void run(String triggeredBy) {
        eodService.processEodPhases(triggeredBy);
    }
}
