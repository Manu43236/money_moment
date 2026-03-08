package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportLoanBookDto {
    private String statusCode;
    private Long loanCount;
    private Double totalLoanAmount;
    private Double totalOutstanding;
}
