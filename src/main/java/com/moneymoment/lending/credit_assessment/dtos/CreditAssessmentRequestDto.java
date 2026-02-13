package com.moneymoment.lending.credit_assessment.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditAssessmentRequestDto {

    private String loanNumber;
    private Double existingEmiObligations; // Optional - customer's existing EMIs
    private String assessedBy; // Employee ID who is assessing
}