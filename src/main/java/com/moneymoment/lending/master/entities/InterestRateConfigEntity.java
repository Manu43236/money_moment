package com.moneymoment.lending.master.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "interest_rate_config")
public class InterestRateConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_type_id", nullable = false)
    private LoanTypesEntity loanType;

    @Column(name = "min_credit_score")
    private Integer minCreditScore;

    @Column(name = "max_credit_score")
    private Integer maxCreditScore;

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "min_loan_amount")
    private Double minLoanAmount;

    @Column(name = "max_loan_amount")
    private Double maxLoanAmount;

    @Column(name = "interest_rate")
    private Double interestRate;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
