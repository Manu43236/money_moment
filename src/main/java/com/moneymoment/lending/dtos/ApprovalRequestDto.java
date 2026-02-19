package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestDto {
    
    private String loanNumber;
    private String action;  // "APPROVE" or "REJECT"
    private String remarks;
    private String approvedByEmployeeId;  // Employee who is approving
}