package com.moneymoment.lending.documents.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDto {

    // Identifiers
    private Long id;
    private String documentNumber;

    // Customer Info (nullable - only if document belongs to customer)
    private Long customerId;
    private String customerNumber;
    private String customerName;

    // Loan Info (nullable - only if document belongs to loan)
    private Long loanId;
    private String loanNumber;

    // Document Type
    private Long documentTypeId;
    private String documentTypeCode;
    private String documentTypeName;

    // File Info
    private String fileName;
    private String fileUrl;
    private String filePath;
    private Long fileSizeKb;
    private String fileType;

    // Status
    private String uploadStatus;
    private String verificationNotes;
    private String verifiedBy;
    private LocalDateTime verifiedAt;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}