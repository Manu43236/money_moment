package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDpdBucketDto {
    private String bucket;        // CURRENT, SMA-0, SMA-1, SMA-2, NPA
    private String dpdRange;      // e.g. "0 days", "1-30 days"
    private Long loanCount;
    private Double outstandingAmount;
    private Double overdueAmount;
}
