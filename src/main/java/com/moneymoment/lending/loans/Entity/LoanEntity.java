package com.moneymoment.lending.loans.Entity;

import java.time.LocalDateTime;

import com.moneymoment.lending.common.entity.BaseEntity;
import com.moneymoment.lending.customers.entities.CustomerEntity;
import com.moneymoment.lending.master.entities.DisbursementModesEntity;
import com.moneymoment.lending.master.entities.LoanPurposesEntity;
import com.moneymoment.lending.master.entities.LoanStatusesEntity;
import com.moneymoment.lending.master.entities.LoanTypesEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
@AllArgsConstructor
@Table(name = "loans")
public class LoanEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // audit fields
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "loan_number", nullable = false, unique = true)
    private String loanNumber;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne
    @JoinColumn(name = "loan_type_id", nullable = false)
    private LoanTypesEntity loanType;

    @ManyToOne
    @JoinColumn(name = "loan_purpose_id", nullable = false)
    private LoanPurposesEntity loanPurpose;

    @ManyToOne
    @JoinColumn(name = "loan_status_id", nullable = false)
    private LoanStatusesEntity loanStatus;

    @ManyToOne
    @JoinColumn(name = "disbursement_mode_id", nullable = true)
    private DisbursementModesEntity disbursementMode;

    @Column(name = "loan_amount", nullable = false)
    private Double loanAmount;

    @Column(name = "interest_rate", nullable = false)
    private Double interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "processing_fee")
    private Double processingFee;

    @Column(name = "emi_amount", nullable = false)
    private Double emiAmount;

    @Column(name = "total_interest", nullable = false)
    private Double totalInterest;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "outstanding_amount", nullable = false)
    private Double outstandingAmount;

    // Dates
    @Column(name = "applied_date", nullable = false)
    private LocalDateTime appliedDate;
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;
    @Column(name = "disbursed_date")
    private LocalDateTime disbursedDate;
    @Column(name = "closed_date")
    private LocalDateTime closedDate;
    @Column(name = "rejected_date")
    private LocalDateTime rejectedDate;
    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Disbursement Details
    @Column(name = "disbursement_account_number")
    private String disbursementAccountNumber;
    @Column(name = "disbursement_ifsc")
    private String disbursementIfsc;

    @Column(name = "repayment_frequency")
    private String repaymentFrequency;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
