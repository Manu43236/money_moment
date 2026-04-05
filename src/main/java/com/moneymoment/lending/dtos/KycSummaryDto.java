package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycSummaryDto {
    private String panNumber;
    private String panName;
    private String aadhaarNumber;
    private String aadhaarName;
    private Boolean nameMatched;
    private Double matchScore;
}
