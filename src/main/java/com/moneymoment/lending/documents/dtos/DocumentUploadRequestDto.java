package com.moneymoment.lending.documents.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUploadRequestDto {
    
    // Use business identifiers, NOT IDs
    private String customerNumber;  // "CUST2026..."
    private String loanNumber;      // "LN2026..."
    
    private String documentTypeCode; // "PAN_CARD"
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSizeKb;
}
