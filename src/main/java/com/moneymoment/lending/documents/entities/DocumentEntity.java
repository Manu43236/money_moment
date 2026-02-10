package com.moneymoment.lending.documents.entities;

import java.time.LocalDateTime;

import com.moneymoment.lending.common.entity.BaseEntity;
import com.moneymoment.lending.common.enums.DocumentStatusEnums;
import com.moneymoment.lending.customers.entities.CustomerEntity;
import com.moneymoment.lending.loans.Entity.LoanEntity;
import com.moneymoment.lending.master.entities.DocumentTypesEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "documents")
public class DocumentEntity extends BaseEntity {

    @Column(name = "document_number", unique = true, nullable = false, length = 50)
    private String documentNumber;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private LoanEntity loan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentTypesEntity documentType;

    // File info
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size_kb", nullable = false)
    private Long fileSizeKb;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    // Status
    @Column(name = "upload_status", length = 20)
    private String uploadStatus = DocumentStatusEnums.UPLOADED;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}