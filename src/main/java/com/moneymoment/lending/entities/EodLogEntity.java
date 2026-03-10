package com.moneymoment.lending.entities;

import java.time.LocalDateTime;

import com.moneymoment.lending.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "eod_logs")
public class EodLogEntity extends BaseEntity {

    @Column(name = "job_id", length = 50)
    private String jobId;

    @Column(name = "run_date", nullable = false)
    private LocalDateTime runDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS, FAILED

    @Column(name = "triggered_by", length = 50)
    private String triggeredBy; // SCHEDULER, MANUAL

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Phase summaries
    @Column(name = "interest_accrued")
    private Double interestAccrued;

    @Column(name = "nach_processed")
    private Integer nachProcessed;

    @Column(name = "nach_bounced")
    private Integer nachBounced;

    @Column(name = "customers_alerted")
    private Integer customersAlerted;

    @Column(name = "total_provision_amount")
    private Double totalProvisionAmount;

    @Column(name = "emis_due_tomorrow")
    private Integer emisDueTomorrow;

    @Column(name = "reconciliation_passed")
    private Boolean reconciliationPassed;

    // Step 1 — DPD
    @Column(name = "total_loans_processed")
    private Integer totalLoansProcessed;

    @Column(name = "total_emis_processed")
    private Integer totalEmisProcessed;

    @Column(name = "emis_marked_overdue")
    private Integer emisMarkedOverdue;

    @Column(name = "emis_already_paid")
    private Integer emisAlreadyPaid;

    // Step 2 — Status
    @Column(name = "loans_marked_active")
    private Integer loansMarkedActive;

    @Column(name = "loans_marked_overdue")
    private Integer loansMarkedOverdue;

    @Column(name = "loans_marked_npa")
    private Integer loansMarkedNpa;

    @Column(name = "loans_stayed_disbursed")
    private Integer loansStayedDisbursed;

    // Step 3 — Penalties
    @Column(name = "penalties_applied")
    private Integer penaltiesApplied;
}
