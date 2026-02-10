package com.moneymoment.lending.loans.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDto {

    // Identifiers
    private Long id;
    private String loanNumber;

    // Customer Info
    private Long customerId;
    private String customerName;
    private String customerNumber;

    // Loan Type & Purpose
    private String loanTypeCode;
    private String loanTypeName;
    private String loanPurposeCode;
    private String loanPurposeName;

    // Loan Details
    private Double loanAmount;
    private Double interestRate;
    private Integer tenureMonths;
    private String purpose;

    // Financial Calculations
    private Double processingFee;
    private Double emiAmount;
    private Double totalInterest;
    private Double totalAmount;
    private Double outstandingAmount;

    // Status
    private String loanStatusCode;
    private String loanStatusName;

    // Dates
    private LocalDateTime appliedDate;
    private LocalDateTime approvedDate;
    private LocalDateTime disbursedDate;
    private LocalDateTime closedDate;
    private LocalDateTime rejectedDate;

    // Rejection
    private String rejectionReason;

    // Disbursement
    private String disbursementModeCode;
    private String disbursementModeName;
    private String disbursementAccountNumber;
    private String disbursementIfsc;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}