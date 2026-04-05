package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponseDto {
    private String sessionId;
    private String reply;
    private String sessionStatus;
    private Long createdCustomerId;
    private String createdCustomerNumber;
    private String createdCustomerName;
    private Long createdLoanId;
    private String createdLoanNumber;
    private KycSummaryDto kycSummary;
}
