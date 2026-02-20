package com.moneymoment.lending.entities;

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
@Table(name = "disbursements")
public class DisbursementEntity extends BaseEntity {

    @Column(name = "disbursement_number", unique = true, nullable = false, length = 50)
    private String disbursementNumber;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursed_by_user_id")
    private UserEntity disbursedByUser;

    // Amount Details
    @Column(name = "disbursement_amount", nullable = false)
    private Double disbursementAmount;

    @Column(name = "processing_fee")
    private Double processingFee;

    @Column(name = "net_disbursement", nullable = false)
    private Double netDisbursement;

    // Beneficiary Details
    @Column(name = "beneficiary_account_number", nullable = false, length = 20)
    private String beneficiaryAccountNumber;

    @Column(name = "beneficiary_ifsc", nullable = false, length = 15)
    private String beneficiaryIfsc;

    @Column(name = "beneficiary_name", nullable = false, length = 100)
    private String beneficiaryName;

    // Disbursement Details
    @Column(name = "disbursement_mode", nullable = false, length = 20)
    private String disbursementMode;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "utr_number", length = 50)
    private String utrNumber;

    // Status
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // Dates
    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Disbursed By (denormalized)
    @Column(name = "disbursed_by_employee_id", length = 20)
    private String disbursedByEmployeeId;

    @Column(name = "disbursed_by_name", length = 100)
    private String disbursedByName;
}