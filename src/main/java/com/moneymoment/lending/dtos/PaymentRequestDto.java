package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    private String loanNumber;
    private Integer emiNumber; // Which EMI customer is paying for
    private Double paymentAmount;
    private String paymentMode; // NACH, UPI, NEFT, RTGS, CASH, CHEQUE
    private LocalDate paymentDate;
    private String transactionId; // Optional - from payment gateway
    private String referenceNumber; // Optional - bank reference
}