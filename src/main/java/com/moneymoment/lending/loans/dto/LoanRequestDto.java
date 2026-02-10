package com.moneymoment.lending.loans.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanRequestDto {
    private Long customerId;
    private String loanTypeCode;
    private String loanPurposeCode;

    private String purpose;
    private String disbursementAccountNumber;
    private String disbursementIfsc;

    private Double loanAmount;
    private Integer tenureMonths;
    // Getters and Setters

}
