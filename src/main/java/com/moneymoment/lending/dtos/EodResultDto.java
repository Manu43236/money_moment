package com.moneymoment.lending.dtos;

import lombok.Data;

@Data
public class EodResultDto {

    // Step 1 — DPD Calculation
    private int totalLoansProcessed;
    private int totalEmisProcessed;
    private int emisMarkedOverdue;
    private int emisAlreadyPaid;

    // Step 2 — Loan Status Update
    private int loansMarkedActive;
    private int loansMarkedOverdue;
    private int loansMarkedNpa;
    private int loansStayedDisbursed;

    // Step 3 — Penalty Application
    private int penaltiesApplied;
    private double totalPenaltyAmount;
}
