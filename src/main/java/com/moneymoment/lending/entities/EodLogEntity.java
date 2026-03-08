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

    @Column(name = "run_date", nullable = false)
    private LocalDateTime runDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS, FAILED

    @Column(name = "triggered_by", length = 50)
    private String triggeredBy; // SCHEDULER, MANUAL

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
