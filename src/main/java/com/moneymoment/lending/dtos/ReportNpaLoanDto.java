package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportNpaLoanDto {
    private String loanNumber;
    private String customerName;
    private String customerNumber;
    private Double loanAmount;
    private Double outstandingAmount;
    private Double overdueAmount;
    private Integer currentDpd;
    private Integer highestDpd;
    private Integer overdueEmis;
}
