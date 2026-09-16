package com.moneymoment.lending.dtos;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BulkPaymentRequestDto {
    private String loanNumber;
    private String paymentMode;
    private LocalDate paymentDate;
    private String transactionId;
    private String referenceNumber;
}
