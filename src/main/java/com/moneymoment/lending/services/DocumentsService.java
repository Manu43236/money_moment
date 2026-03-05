package com.moneymoment.lending.services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.moneymoment.lending.common.response.PagedResponse;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.enums.DocumentStatusEnums;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.exception.ValidationException;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.dtos.DocumentResponseDto;
import com.moneymoment.lending.dtos.DocumentUploadRequestDto;
import com.moneymoment.lending.dtos.DocumentVerifyDto;
import com.moneymoment.lending.entities.DocumentEntity;
import com.moneymoment.lending.master.MasterService;
import com.moneymoment.lending.repos.CustomerRepository;
import com.moneymoment.lending.repos.DocumentRepository;
import com.moneymoment.lending.repos.LoanRepo;

@Service
public class DocumentsService {

    private final LoanRepo loanRepo;
    private final CustomerRepository customerRepository;
    private final MasterService masterService;
    private final DocumentRepository documentRepository;
    private final R2StorageService r2StorageService;

    DocumentsService(LoanRepo loanRepo, CustomerRepository customerRepository, MasterService masterService,
            DocumentRepository documentRepository, R2StorageService r2StorageService) {
        this.loanRepo = loanRepo;
        this.customerRepository = customerRepository;
        this.masterService = masterService;
        this.documentRepository = documentRepository;
        this.r2StorageService = r2StorageService;
    }

    @Transactional
    public DocumentResponseDto uploadDocument(MultipartFile file, String customerNumber, String loanNumber,
            String documentTypeCode) {

        DocumentEntity documentEntity = new DocumentEntity();

        // Step 1: Validate and set customer/loan
        if (loanNumber != null && !loanNumber.isEmpty()) {
            var loan = loanRepo.findByLoanNumber(loanNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));
            documentEntity.setLoan(loan);
            documentEntity.setCustomer(loan.getCustomer());

        } else if (customerNumber != null && !customerNumber.isEmpty()) {
            var customer = customerRepository.findByCustomerNumber(customerNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerNumber", customerNumber));
            documentEntity.setCustomer(customer);
            documentEntity.setLoan(null);

        } else {
            throw new ValidationException("Either loanNumber or customerNumber must be provided");
        }

        // Step 2: Fetch and validate document type
        var documentType = masterService.getDocumentTypesByCode(documentTypeCode);

        if (documentEntity.getLoan() != null) {
            if (!documentType.getApplicableFor().equals("LOAN") && !documentType.getApplicableFor().equals("BOTH")) {
                throw new ValidationException(
                        documentType.getName() + " cannot be uploaded for loan. It's only for customer.");
            }
        } else {
            if (!documentType.getApplicableFor().equals("CUSTOMER")
                    && !documentType.getApplicableFor().equals("BOTH")) {
                throw new ValidationException(
                        documentType.getName() + " cannot be uploaded for customer. It's only for loan.");
            }
        }

        // Step 3: Generate document number
        String docNumber = documentEntity.getLoan() != null
                ? NumberGenerator.numberGeneratorWithPrifix(AppConstants.LOAN_DOCUMENT_NUMBER_PREFIX)
                : NumberGenerator.numberGeneratorWithPrifix(AppConstants.USER_DOCUMENT_NUMBER_PREFIX);
        documentEntity.setDocumentNumber(docNumber);

        // Step 4: Build Cloudinary key and upload
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        String ext = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";

        // image/* → images folder, everything else → docs folder
        String fileCategory = contentType.startsWith("image/") ? "images" : "docs";

        // Last 4 chars of customer/loan number for folder name
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String cloudinaryKey;
        String displayFileName;

        if (documentEntity.getLoan() != null) {
            String last4 = loanNumber.length() >= 4
                    ? loanNumber.substring(loanNumber.length() - 4) : loanNumber;
            // public_id WITHOUT extension — Cloudinary appends it automatically
            cloudinaryKey = "loans/" + fileCategory + "/ln_" + last4
                    + "/" + documentTypeCode + "_" + timestamp;
        } else {
            String last4 = customerNumber.length() >= 4
                    ? customerNumber.substring(customerNumber.length() - 4) : customerNumber;
            cloudinaryKey = "customers/" + fileCategory + "/cust_" + last4
                    + "/" + documentTypeCode + "_" + timestamp;
        }

        displayFileName = cloudinaryKey.substring(cloudinaryKey.lastIndexOf('/') + 1) + ext;

        String fileUrl;
        try {
            fileUrl = r2StorageService.upload(cloudinaryKey, file);
        } catch (IOException e) {
            throw new com.moneymoment.lending.common.exception.BusinessLogicException(
                    "Failed to upload file to storage: " + e.getMessage());
        }

        // Step 5: Set document fields
        documentEntity.setDocumentType(documentType);
        documentEntity.setFileName(displayFileName);
        documentEntity.setFilePath(cloudinaryKey);
        documentEntity.setFileUrl(fileUrl);
        documentEntity.setFileType(contentType);
        documentEntity.setFileSizeKb(file.getSize() / 1024);
        documentEntity.setUploadStatus(DocumentStatusEnums.UPLOADED);

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
    @Transactional
    public DocumentResponseDto getDocumentById(Long id) {
        var documentEntity = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id.toString()));

        return toDto(documentEntity);
    }

    // get all docs for customer
    @Transactional
    public List<DocumentResponseDto> getDocumentsByCustomerNumber(String customerNumber) {
        var customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerNumber", customerNumber));
        var documents = documentRepository.findByCustomerId(customer.getId());
        return documents.stream().map(this::toDto).toList();
    }

    // get document by customer number and document type code
    @Transactional
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
    @Transactional
    public PagedResponse<DocumentResponseDto> getDocumentsByLoanNumber(String loanNumber, int page, int size) {
        var loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.of(documentRepository.findByLoanId(loan.getId(), pageable).map(this::toDto));
    }

    // get document by loan number and document type code
    @Transactional
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
    @Transactional
    public DocumentResponseDto getDocumentByDocumentNumber(String documentNumber) {
        var documentEntity = documentRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "documentNumber", documentNumber));
        return toDto(documentEntity);
    }

    // get all documents
    @Transactional
    public PagedResponse<DocumentResponseDto> getAllDocuments(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.of(documentRepository.findAll(pageable).map(this::toDto));
    }

    @Transactional
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

    @Transactional
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
    @Transactional
    public List<DocumentResponseDto> getDocumentsForVerification() {
        var documents = documentRepository.findAll().stream()
                .filter(doc -> doc.getUploadStatus() != DocumentStatusEnums.REJECTED)
                .toList();
        return documents.stream().map(this::toDto).toList();
    }

    // get customer's documents which are verification is not rejected
    @Transactional
    public PagedResponse<DocumentResponseDto> getCustomerDocumentsForVerification(String customerNumber, int page, int size) {
        var customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerNumber", customerNumber));
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.of(documentRepository
                .findByCustomerIdAndUploadStatusNot(customer.getId(), DocumentStatusEnums.REJECTED, pageable)
                .map(this::toDto));
    }
}
