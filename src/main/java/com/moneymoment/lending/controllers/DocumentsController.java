package com.moneymoment.lending.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.DocumentResponseDto;
import com.moneymoment.lending.dtos.DocumentVerifyDto;
import com.moneymoment.lending.services.DocumentsService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentsController {

    private final DocumentsService documentsService;

    DocumentsController(DocumentsService documentsService) {
        this.documentsService = documentsService;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<PagedResponse<DocumentResponseDto>>> getAllDocs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(documentsService.getAllDocuments(page, size), "Documents fetched successfully"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocuments(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String customerNumber,
            @RequestParam(required = false) String loanNumber,
            @RequestParam String documentTypeCode) {

        return ResponseEntity.ok(ApiResponse.success(
                documentsService.uploadDocument(file, customerNumber, loanNumber, documentTypeCode),
                "Document uploaded successfully"));
    }

    // update docuement
    // @PutMapping("/{id}")
    // public ResponseEntity<ApiResponse<DocumentResponseDto>>
    // putMethodName(@PathVariable String id, @RequestBody DocumentUploadRequestDto
    // entity) {
    // //TODO: process PUT request

    // return
    // ResponseEntity.ok(ApiResponse.success(documentsService.updateDocument(id,
    // entity), "Document updated successfully"));
    // }

    // get documents by customer number
    @GetMapping("/customer/{customerNumber}")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentResponseDto>>> getNotRejectedDocumentsByCustomerNumber(
            @PathVariable String customerNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity
                .ok(ApiResponse.success(documentsService.getCustomerDocumentsForVerification(customerNumber, page, size),
                        "Documents fetched successfully"));
    }

    // get documents by loan number
    @GetMapping("/loan/{loanNumber}")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentResponseDto>>> getDocumentsByLoanNumber(
            @PathVariable String loanNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(documentsService.getDocumentsByLoanNumber(loanNumber, page, size),
                "Documents fetched successfully"));
    }

    // verify document
    @PutMapping("/{documentNumber}/verify")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> verifyDocument(@PathVariable String documentNumber,
            @RequestBody DocumentVerifyDto verifyDto) {
        return ResponseEntity.ok(ApiResponse.success(documentsService.verifyDocument(documentNumber, verifyDto),
                "Document verified successfully"));
    }

}
