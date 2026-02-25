package com.moneymoment.lending.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.moneymoment.lending.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "loan_penalties")
public class LoanPenaltyEntity extends BaseEntity {

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emi_schedule_id")
    private EmiScheduleEntity emiSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_config_id", nullable = false)
    private PenaltyConfigEntity penaltyConfig;

    // Penalty Details
    @Column(name = "penalty_code", nullable = false, length = 30)
    private String penaltyCode;

    @Column(name = "penalty_name", nullable = false, length = 100)
    private String penaltyName;

    @Column(name = "penalty_amount", nullable = false)
    private Double penaltyAmount;

    // Context
    @Column(name = "base_amount")
    private Double baseAmount;

    @Column(name = "days_overdue")
    private Integer daysOverdue;

    // Waiver
    @Column(name = "is_waived")
    private Boolean isWaived;

    @Column(name = "waived_amount")
    private Double waivedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waived_by_user_id")
    private UserEntity waivedByUser;

    @Column(name = "waived_at")
    private LocalDateTime waivedAt;

    @Column(name = "waiver_reason", columnDefinition = "TEXT")
    private String waiverReason;

    // Payment
    @Column(name = "is_paid")
    private Boolean isPaid;

    @Column(name = "paid_amount")
    private Double paidAmount;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    // Applied Info
    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(name = "applied_by", length = 50)
    private String appliedBy; // 'SYSTEM' or employee ID

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}