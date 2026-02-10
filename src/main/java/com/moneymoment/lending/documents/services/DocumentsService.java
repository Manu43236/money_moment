package com.moneymoment.lending.documents.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.enums.DocumentStatusEnums;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.exception.ValidationException;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.customers.repos.CustomerRepository;
import com.moneymoment.lending.documents.dtos.DocumentResponseDto;
import com.moneymoment.lending.documents.dtos.DocumentUploadRequestDto;
import com.moneymoment.lending.documents.dtos.DocumentVerifyDto;
import com.moneymoment.lending.documents.entities.DocumentEntity;
import com.moneymoment.lending.documents.repos.DocumentRepository;
import com.moneymoment.lending.loans.repos.LoanRepo;
import com.moneymoment.lending.master.MasterService;

@Service
public class DocumentsService {

    private final LoanRepo loanRepo;
    private final CustomerRepository customerRepository;
    private final MasterService masterService;
    private final DocumentRepository documentRepository;

    DocumentsService(LoanRepo loanRepo, CustomerRepository customerRepository, MasterService masterService,
            DocumentRepository documentRepository) {
        this.loanRepo = loanRepo;
        this.customerRepository = customerRepository;
        this.masterService = masterService;
        this.documentRepository = documentRepository;

    }

    public DocumentResponseDto uploadDocument(DocumentUploadRequestDto request) {

        DocumentEntity documentEntity = new DocumentEntity();

        // Step 1: Validate and set customer/loan
        if (request.getLoanNumber() != null && !request.getLoanNumber().isEmpty()) {
            // Upload for loan
            var loan = loanRepo.findByLoanNumber(request.getLoanNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", request.getLoanNumber()));

            documentEntity.setLoan(loan);
            documentEntity.setCustomer(loan.getCustomer());

        } else if (request.getCustomerNumber() != null && !request.getCustomerNumber().isEmpty()) {
            // Upload for customer
            var customer = customerRepository.findByCustomerNumber(request.getCustomerNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerNumber",
                            request.getCustomerNumber()));

            documentEntity.setCustomer(customer);
            documentEntity.setLoan(null);

        } else {
            throw new ValidationException("Either loanNumber or customerNumber must be provided");
        }

        // Step 2: Fetch document type
        var documentType = masterService.getDocumentTypesByCode(request.getDocumentTypeCode());

        // Step 3: Validate applicableFor
        if (documentEntity.getLoan() != null) {
            // Uploading for loan - validate document type allows it
            if (!documentType.getApplicableFor().equals("LOAN")
                    && !documentType.getApplicableFor().equals("BOTH")) {
                throw new ValidationException(
                        documentType.getName() + " cannot be uploaded for loan. It's only for customer.");
            }
        } else {
            // Uploading for customer - validate document type allows it
            if (!documentType.getApplicableFor().equals("CUSTOMER")
                    && !documentType.getApplicableFor().equals("BOTH")) {
                throw new ValidationException(
                        documentType.getName() + " cannot be uploaded for customer. It's only for loan.");
            }
        }

        // Step 4: Generate document number based on context
        if (documentEntity.getLoan() != null) {
            documentEntity.setDocumentNumber(
                    NumberGenerator.numberGeneratorWithPrifix(AppConstants.LOAN_DOCUMENT_NUMBER_PREFIX));
        } else {
            documentEntity.setDocumentNumber(
                    NumberGenerator.numberGeneratorWithPrifix(AppConstants.USER_DOCUMENT_NUMBER_PREFIX));
        }

        // Step 5: Set document type and file info
        documentEntity.setDocumentType(documentType);
        documentEntity.setFileName(request.getFileName());
        documentEntity.setFileUrl(request.getFileUrl());
        documentEntity.setFilePath(request.getFileUrl()); // For now, same as fileUrl
        documentEntity.setFileType(request.getFileType());
        documentEntity.setFileSizeKb(request.getFileSizeKb());

        // Step 6: Set status
        documentEntity.setUploadStatus(DocumentStatusEnums.UPLOADED);

        // Step 7: Save and return
        documentEntity = documentRepository.save(documentEntity);

        return toDto(documentEntity);
    }

    private DocumentResponseDto toDto(DocumentEntity entity) {
        DocumentResponseDto dto = new DocumentResponseDto();

        // Identifiers
        dto.setId(entity.getId());
        dto.setDocumentNumber(entity.getDocumentNumber());

        // Customer info (if present)
        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getId());
            dto.setCustomerNumber(entity.getCustomer().getCustomerNumber());
            dto.setCustomerName(entity.getCustomer().getName());
        }

        // Loan info (if present)
        if (entity.getLoan() != null) {
            dto.setLoanId(entity.getLoan().getId());
            dto.setLoanNumber(entity.getLoan().getLoanNumber());
        }

        // Document type
        dto.setDocumentTypeId(entity.getDocumentType().getId());
        dto.setDocumentTypeCode(entity.getDocumentType().getCode());
        dto.setDocumentTypeName(entity.getDocumentType().getName());

        // File info
        dto.setFileName(entity.getFileName());
        dto.setFileUrl(entity.getFileUrl());
        dto.setFilePath(entity.getFilePath());
        dto.setFileSizeKb(entity.getFileSizeKb());
        dto.setFileType(entity.getFileType());

        // Status
        dto.setUploadStatus(entity.getUploadStatus());
        dto.setVerificationNotes(entity.getVerificationNotes());
        dto.setVerifiedBy(entity.getVerifiedBy());
        dto.setVerifiedAt(entity.getVerifiedAt());

        // Audit
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());

        return dto;
    }

    // get doc by id
    public DocumentResponseDto getDocumentById(Long id) {
        var documentEntity = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id.toString()));

        return toDto(documentEntity);
    }

    // get all docs for customer
    public List<DocumentResponseDto> getDocumentsByCustomerNumber(String customerNumber) {
        var customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerNumber", customerNumber));
        var documents = documentRepository.findByCustomerId(customer.getId());
        return documents.stream().map(this::toDto).toList();
    }

    // get document by customer number and document type code
    public List<DocumentResponseDto> getDocumentsByCustomerNumberAndDocumentTypeCode(String customerNumber,
            String documentTypeCode) {
        var customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerNumber", customerNumber));
        var documentType = masterService.getDocumentTypesByCode(documentTypeCode);
        var documents = documentRepository.findByCustomerId(customer.getId()).stream()
                .filter(doc -> doc.getDocumentType().getId().equals(documentType.getId())).toList();
        return documents.stream().map(this::toDto).toList();
    }

    // get all docs for loan
    public List<DocumentResponseDto> getDocumentsByLoanNumber(String loanNumber) {
        var loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));
        var documents = documentRepository.findByLoanId(loan.getId());
        return documents.stream().map(this::toDto).toList();
    }

    // get document by loan number and document type code
    public List<DocumentResponseDto> getDocumentsByLoanNumberAndDocumentTypeCode(String loanNumber,
            String documentTypeCode) {
        var loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));
        var documentType = masterService.getDocumentTypesByCode(documentTypeCode);
        var documents = documentRepository.findByLoanId(loan.getId()).stream()
                .filter(doc -> doc.getDocumentType().getId().equals(documentType.getId())).toList();
        return documents.stream().map(this::toDto).toList();
    }

    // get document by document number
    public DocumentResponseDto getDocumentByDocumentNumber(String documentNumber) {
        var documentEntity = documentRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "documentNumber", documentNumber));
        return toDto(documentEntity);
    }

    // get all documents
    public List<DocumentResponseDto> getAllDocuments() {
        var documents = documentRepository.findAll();
        return documents.stream().map(this::toDto).toList();
    }

    public DocumentResponseDto updateDocument(String id, DocumentUploadRequestDto entity) {
        var documentEntity = documentRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        // Update the document entity with values from the DTO
        documentEntity.setDocumentType(masterService.getDocumentTypesByCode(entity.getDocumentTypeCode()));
        // documentEntity.setUploadedBy(documentEntity.getUploadedBy());
        documentEntity.setUpdatedAt(LocalDateTime.now());
        // documentEntity.setUploadStatus(entity.getUploadStatus());
        // documentEntity.setVerificationNotes(entity.getVerificationNotes());
        // documentEntity.setVerifiedBy(entity.getVerifiedBy());
        // documentEntity.setVerifiedAt(entity.getVerifiedAt());

        // Update audit fields
        documentEntity.setUpdatedAt(LocalDateTime.now());
        // documentEntity.setUpdatedBy(entity.getUpdatedBy());

        var updatedDocument = documentRepository.save(documentEntity);
        return toDto(updatedDocument);
    }

    public DocumentResponseDto verifyDocument(String documentNumber, DocumentVerifyDto verifyDto) {
        var documentEntity = documentRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "documentNumber", documentNumber));

        // Update verification fields
        documentEntity.setVerifiedBy(verifyDto.getVerifiedBy());
        documentEntity.setVerificationNotes(verifyDto.getVerificationNotes());
        documentEntity.setUploadStatus(verifyDto.getUploadStatus());
        documentEntity.setVerifiedAt(LocalDateTime.now());

        System.out.println("UPLOAD STATUS" + verifyDto);
        var updatedDocument = documentRepository.save(documentEntity);
        return toDto(updatedDocument);
    }

    // get documents which are verification is not rejected
    public List<DocumentResponseDto> getDocumentsForVerification() {
        var documents = documentRepository.findAll().stream()
                .filter(doc -> doc.getUploadStatus() != DocumentStatusEnums.REJECTED)
                .toList();
        return documents.stream().map(this::toDto).toList();
    }

    // get customer's documents which are verification is not rejected
    public List<DocumentResponseDto> getCustomerDocumentsForVerification(String customerNumber) {
        var customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerNumber", customerNumber));
        var documents = documentRepository.findByCustomerId(customer.getId()).stream()
                .filter((doc) -> !doc.getUploadStatus().equals(DocumentStatusEnums.REJECTED))
                .toList();

        return documents.stream().map(this::toDto).toList();
    }
}
