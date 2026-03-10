package com.moneymoment.lending.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class EodJobStatus {
    private String jobId;
    private String status; // IDLE, RUNNING, COMPLETED, FAILED
    private String triggeredBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds;
    private int currentPhaseNumber;
    private List<EodPhaseResult> phases;
    private String error;
    private String lastCompletedJobId;
    private LocalDateTime lastCompletedAt;
}
