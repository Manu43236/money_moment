package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponseDto {

    // Identifiers
    private Long id;

    // Loan Info
    private Long loanId;
    private String loanNumber;
    private Double loanAmount;

    // Customer Info
    private Long customerId;
    private String customerNumber;
    private String customerName;

    // Approver Info
    private Long approvedByUserId;
    private String approvedByEmployeeId;
    private String approvedByName;
    private String roleCode;
    private Integer approvalLevel;

    // Action
    private String action;
    private String remarks;
    private LocalDateTime actionTakenAt;

    // Audit
    private LocalDateTime createdAt;
}