package com.moneymoment.lending.dtos;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class EodPhaseResult {
    private int phaseNumber;
    private String phaseName;
    private String description;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds;
    private Map<String, Object> metrics = new HashMap<>();
    private String error;

    public EodPhaseResult(int phaseNumber, String phaseName, String description) {
        this.phaseNumber = phaseNumber;
        this.phaseName = phaseName;
        this.description = description;
        this.status = "PENDING";
    }
}
