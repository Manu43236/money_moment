package com.moneymoment.lending.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EodLogResponseDto {
    private Long id;
    private String jobId;
    private LocalDateTime runDate;
    private LocalDateTime completedAt;
    private Long durationSeconds;
    private String status;
    private String triggeredBy;
    private String errorMessage;

    // Phase 2 — DPD
    private Integer totalLoansProcessed;
    private Integer totalEmisProcessed;
    private Integer emisMarkedOverdue;
    private Integer emisAlreadyPaid;

    // Phase 2 — Status
    private Integer loansMarkedActive;
    private Integer loansMarkedOverdue;
    private Integer loansMarkedNpa;
    private Integer loansStayedDisbursed;

    // Phase 2 — Penalties
    private Integer penaltiesApplied;

    // Phase 3 — Interest
    private Double interestAccrued;

    // Phase 4 — NACH
    private Integer nachProcessed;
    private Integer nachBounced;

    // Phase 5 — Collections
    private Integer customersAlerted;

    // Phase 6 — Provisioning
    private Double totalProvisionAmount;

    // Phase 10 — Next Day
    private Integer emisDueTomorrow;

    // Phase 11 — Verification
    private Boolean reconciliationPassed;
}
