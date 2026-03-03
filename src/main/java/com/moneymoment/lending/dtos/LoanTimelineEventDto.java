package com.moneymoment.lending.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanTimelineEventDto {

    private String eventType;    // LOAN_APPLIED, DOCUMENT_UPLOADED, etc.
    private String title;        // Human readable title
    private String description;  // Details of the event
    private String performedBy;  // Who did it
    private LocalDateTime timestamp;
    private String status;       // SUCCESS, PENDING, FAILED, INFO
    private String metadata;     // Extra info (amount, EMI number etc.)
}
