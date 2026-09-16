package com.moneymoment.lending.dtos;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PreClosureRequestDto {
    private String paymentMode;
    private LocalDate paymentDate;
    private String transactionId;
    private String referenceNumber;
}
