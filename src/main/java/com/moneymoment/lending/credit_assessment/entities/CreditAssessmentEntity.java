package com.moneymoment.lending.credit_assessment.entities;

import java.time.LocalDateTime;

import com.moneymoment.lending.common.entity.BaseEntity;
import com.moneymoment.lending.customers.entities.CustomerEntity;
import com.moneymoment.lending.loans.Entity.LoanEntity;

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
@Table(name = "credit_assessments")
public class CreditAssessmentEntity extends BaseEntity {

    @Column(name = "assessment_number", unique = true, nullable = false, length = 50)
    private String assessmentNumber;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    // Credit Score
    @Column(name = "credit_score")
    private Double creditScore;

    @Column(name = "credit_score_source", length = 50)
    private String creditScoreSource;

    // Income & Obligations
    @Column(name = "monthly_income", nullable = false)
    private Double monthlyIncome;

    @Column(name = "existing_emi_obligations")
    private Double existingEmiObligations = 0.0;

    @Column(name = "proposed_emi", nullable = false)
    private Double proposedEmi;

    // Ratios
    @Column(name = "dti_ratio")
    private Double dtiRatio;

    @Column(name = "foir")
    private Double foir;

    // Assessment Result
    @Column(name = "risk_category", length = 20)
    private String riskCategory;

    @Column(name = "is_eligible")
    private Boolean isEligible;

    @Column(name = "recommendation", length = 20)
    private String recommendation;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // Assessed By
    @Column(name = "assessed_by", length = 100)
    private String assessedBy;

    @Column(name = "assessed_at")
    private LocalDateTime assessedAt;
}