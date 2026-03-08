package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportCollectionDto {
    // Summary stats
    private Long totalEmisDue;
    private Long emisCollected;
    private Long emisPending;
    private Long emisOverdue;
    private Double totalAmountDue;
    private Double totalAmountCollected;
    private Double collectionEfficiency; // (collected / due) * 100
}
