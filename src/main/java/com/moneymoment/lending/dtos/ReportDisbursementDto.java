package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDisbursementDto {
    private String loanNumber;
    private String customerName;
    private String customerNumber;
    private String loanType;
    private Double loanAmount;
    private Double processingFee;
    private Double interestRate;
    private Integer tenureMonths;
    private Double emiAmount;
    private LocalDateTime disbursedDate;
}
