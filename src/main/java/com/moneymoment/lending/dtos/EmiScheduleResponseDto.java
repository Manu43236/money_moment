package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmiScheduleResponseDto {

    // Identifiers
    private Long id;

    // Loan Info
    private Long loanId;
    private String loanNumber;

    // Customer Info
    private Long customerId;
    private String customerNumber;
    private String customerName;

    // EMI Details
    private Integer emiNumber;
    private LocalDate dueDate;

    // Amount Breakdown
    private Double principalAmount;
    private Double interestAmount;
    private Double emiAmount;

    // Outstanding
    private Double outstandingPrincipal;

    // Status
    private String status;

    // Payment Tracking
    private LocalDate paidDate;
    private Double amountPaid;
    private Double shortfallAmount;

    // DPD
    private Integer daysPastDue;

    // Audit
    private LocalDateTime createdAt;
}