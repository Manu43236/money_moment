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
@Table(name = "loan_approvals")
public class LoanApprovalEntity extends BaseEntity {
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id", nullable = false)
    private UserEntity approvedByUser;
    
    // Approver Info (Denormalized for audit)
    @Column(name = "approved_by_employee_id", nullable = false, length = 20)
    private String approvedByEmployeeId;
    
    @Column(name = "approved_by_name", nullable = false, length = 100)
    private String approvedByName;
    
    // Approval Details
    @Column(name = "approval_level", nullable = false)
    private Integer approvalLevel;
    
    @Column(name = "role_code", nullable = false, length = 30)
    private String roleCode;
    
    // Action
    @Column(name = "action", nullable = false, length = 20)
    private String action;  // APPROVE, REJECT
    
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
    
    // Loan Amount Snapshot
    @Column(name = "loan_amount", nullable = false)
    private Double loanAmount;
    
    // Timestamps
    @Column(name = "action_taken_at")
    private LocalDateTime actionTakenAt;
}