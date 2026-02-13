package com.moneymoment.lending.credit_assessment.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditAssessmentResponseDto {

    // Identifiers
    private Long id;
    private String assessmentNumber;

    // Loan & Customer Info
    private Long loanId;
    private String loanNumber;
    private Long customerId;
    private String customerNumber;
    private String customerName;

    // Credit Score
    private Double creditScore;
    private String creditScoreSource;

    // Income & Obligations
    private Double monthlyIncome;
    private Double existingEmiObligations;
    private Double proposedEmi;
    private Double loanAmount;

    // Ratios
    private Double dtiRatio;
    private Double foir;

    // Assessment Result
    private String riskCategory;
    private Boolean isEligible;
    private String recommendation;
    private String remarks;

    // Assessed By
    private String assessedBy;
    private LocalDateTime assessedAt;

    // Audit
    private LocalDateTime createdAt;
}
