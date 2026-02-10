package com.moneymoment.lending.common.utils;

import com.moneymoment.lending.master.entities.ProcessingFeeConfigEntity;

public class ProcessingChargeUtils {

    @SuppressWarnings("unlikely-arg-type")
    public
    static Double calculateProcessingFee(ProcessingFeeConfigEntity processingFeeConfigEntity, double loanAmount) {
        {
            double rawFee = 0.0;
            if (processingFeeConfigEntity.getFeeType().equals("PERCENTAGE")) {
                // Placeholder for actual percentage calculation
                rawFee = (loanAmount * processingFeeConfigEntity.getFeeValue()) / 100;
            } else {
                rawFee = processingFeeConfigEntity.getFeeValue();
            }

            return rawFee < processingFeeConfigEntity.getMinFee() ? processingFeeConfigEntity.getMinFee()
                    : rawFee > processingFeeConfigEntity.getMaxFee() ? processingFeeConfigEntity.getMaxFee() : rawFee;
        }
    }
}
