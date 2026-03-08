package com.moneymoment.lending.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EodLogResponseDto {
    private Long id;
    private LocalDateTime runDate;
    private String status;
    private String triggeredBy;
    private String errorMessage;

    private Integer totalLoansProcessed;
    private Integer totalEmisProcessed;
    private Integer emisMarkedOverdue;
    private Integer emisAlreadyPaid;

    private Integer loansMarkedActive;
    private Integer loansMarkedOverdue;
    private Integer loansMarkedNpa;
    private Integer loansStayedDisbursed;

    private Integer penaltiesApplied;
}
