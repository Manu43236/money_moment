package com.moneymoment.lending.dtos;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Data;

@Data
public class EodPhaseDetailDto {
    private Integer phaseNumber;
    private String phaseName;
    private String description;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds;
    private Map<String, Object> metrics;
    private String error;
}
