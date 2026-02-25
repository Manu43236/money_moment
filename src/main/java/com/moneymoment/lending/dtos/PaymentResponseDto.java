package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    // Payment Details
    private Long id;
    private String paymentNumber;

    // Loan Info
    private Long loanId;
    private String loanNumber;

    // Customer Info
    private Long customerId;
    private String customerNumber;
    private String customerName;

    // EMI Info
    private Long emiScheduleId;
    private Integer emiNumber;
    private LocalDate emiDueDate;
    private Double emiAmount;

    // Payment Details
    private LocalDate paymentDate;
    private Double paymentAmount;
    private String paymentMode;
    private String transactionId;
    private String referenceNumber;

    // Payment Classification
    private String paymentType; // FULL, PARTIAL, EXCESS
    private Double excessAmount;

    // Status
    private String paymentStatus;
    private String emiStatus; // Updated EMI status

    // Outstanding
    private Double shortfallAmount;
    private Double loanOutstandingAmount;

    // Audit
    private LocalDateTime createdAt;
}