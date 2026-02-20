package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementResponseDto {

    // Identifiers
    private Long id;
    private String disbursementNumber;

    // Loan Info
    private Long loanId;
    private String loanNumber;

    // Customer Info
    private Long customerId;
    private String customerNumber;
    private String customerName;

    // Amount Details
    private Double disbursementAmount;
    private Double processingFee;
    private Double netDisbursement;

    // Beneficiary Details
    private String beneficiaryAccountNumber;
    private String beneficiaryIfsc;
    private String beneficiaryName;

    // Disbursement Details
    private String disbursementMode;
    private String transactionId;
    private String utrNumber;

    // Status
    private String status;
    private String failureReason;

    // Dates
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;

    // Disbursed By
    private String disbursedByEmployeeId;
    private String disbursedByName;

    // Audit
    private LocalDateTime createdAt;
}