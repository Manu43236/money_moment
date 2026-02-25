package com.moneymoment.lending.entities;

import java.time.LocalDate;

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
@Table(name = "emi_payments")
public class EmiPaymentEntity extends BaseEntity {

    @Column(name = "payment_number", unique = true, nullable = false, length = 50)
    private String paymentNumber;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emi_schedule_id", nullable = false)
    private EmiScheduleEntity emiSchedule;

    // Payment Details
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_amount", nullable = false)
    private Double paymentAmount;

    @Column(name = "payment_mode", nullable = false, length = 30)
    private String paymentMode; // NACH, UPI, NEFT, RTGS, CASH, CHEQUE

    // Transaction Info
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    // Payment Classification
    @Column(name = "payment_type", nullable = false, length = 30)
    private String paymentType; // FULL, PARTIAL, EXCESS, ADVANCE

    @Column(name = "excess_amount")
    private Double excessAmount;

    // Status
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus; // SUCCESS, FAILED, PENDING, BOUNCED

    @Column(name = "bounce_reason", columnDefinition = "TEXT")
    private String bounceReason;

    // Processed By
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private UserEntity processedByUser;
}