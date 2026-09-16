package com.moneymoment.lending.dtos;

import lombok.Data;

@Data
public class BulkPaymentResponseDto {
    private String loanNumber;
    private int emisCleared;
    private double penaltiesCleared;
    private double totalAmountPaid;
    private String newLoanStatus;
}
