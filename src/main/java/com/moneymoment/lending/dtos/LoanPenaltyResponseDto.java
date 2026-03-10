package com.moneymoment.lending.dtos;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LoanPenaltyResponseDto {
    private Long id;

    // Loan & Customer (denormalized)
    private Long loanId;
    private String loanNumber;
    private Long customerId;
    private String customerName;

    // EMI reference
    private Long emiScheduleId;
    private Integer emiNumber;
    private LocalDate emiDueDate;

    // Penalty details
    private String penaltyCode;
    private String penaltyName;
    private Double penaltyAmount;
    private Double baseAmount;
    private Integer daysOverdue;

    // Waiver
    private Boolean isWaived;
    private Double waivedAmount;
    private LocalDateTime waivedAt;
    private String waiverReason;

    // Payment
    private Boolean isPaid;
    private Double paidAmount;
    private LocalDate paidDate;

    // Applied info
    private LocalDate appliedDate;
    private String appliedBy;
    private String remarks;
}
