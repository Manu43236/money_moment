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
@Table(name = "emi_schedule")
public class EmiScheduleEntity extends BaseEntity {

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    // EMI Details
    @Column(name = "emi_number", nullable = false)
    private Integer emiNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // Amount Breakdown
    @Column(name = "principal_amount", nullable = false)
    private Double principalAmount;

    @Column(name = "interest_amount", nullable = false)
    private Double interestAmount;

    @Column(name = "emi_amount", nullable = false)
    private Double emiAmount;

    // Outstanding
    @Column(name = "outstanding_principal", nullable = false)
    private Double outstandingPrincipal;

    // Status
    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, PAID, PARTIALLY_PAID, OVERDUE

    // Payment Tracking
    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "amount_paid")
    private Double amountPaid;

    @Column(name = "shortfall_amount")
    private Double shortfallAmount;

    // DPD
    @Column(name = "days_past_due")
    private Integer daysPastDue;
}