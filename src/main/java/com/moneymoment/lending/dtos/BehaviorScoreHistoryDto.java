package com.moneymoment.lending.dtos;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BehaviorScoreHistoryDto {
    private Long id;
    private Double previousScore;
    private Double newScore;
    private Double scoreChange;
    private String triggerSource;
    private String reason;
    private LocalDateTime createdAt;
}
