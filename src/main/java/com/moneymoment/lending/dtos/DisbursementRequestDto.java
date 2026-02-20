package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementRequestDto {

    private String loanNumber;
    private String disbursementMode; // "NEFT", "RTGS", "IMPS"
    private String disbursedByEmployeeId; // Employee initiating disbursement
}