package com.moneymoment.lending.documents.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentVerifyDto {
    private String documentNumber;
    private String verifiedBy;
    private String verificationNotes;
    private String uploadStatus; // "VERIFIED", "REJECTED", "PENDING"
    // Getters and Setters
}
