package com.moneymoment.lending.documents.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.documents.dtos.DocumentResponseDto;
import com.moneymoment.lending.documents.dtos.DocumentUploadRequestDto;
import com.moneymoment.lending.documents.dtos.DocumentVerifyDto;
import com.moneymoment.lending.documents.services.DocumentsService;

import java.util.List;

import javax.swing.text.Document;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/documents")
public class DocumentsController {

    private final DocumentsService documentsService;

    DocumentsController(DocumentsService documentsService) {
        this.documentsService = documentsService;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> getMethodName() {
        return ResponseEntity.ok(ApiResponse.success(documentsService.getAllDocuments(), "API is working"));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<DocumentResponseDto>> postMethodName(
            @RequestBody DocumentUploadRequestDto request) {

        return ResponseEntity
                .ok(ApiResponse.success(documentsService.uploadDocument(request), "Document uploaded successfully"));
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
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> getNotRejectedDocumentsByCustomerNumber(
            @PathVariable String customerNumber) {
        return ResponseEntity
                .ok(ApiResponse.success(documentsService.getCustomerDocumentsForVerification(customerNumber),
                        "Documents fetched successfully"));
    }

    // get documents by loan number
    @GetMapping("/loan/{loanNumber}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> getDocumentsByLoanNumber(
            @PathVariable String loanNumber) {
        return ResponseEntity.ok(ApiResponse.success(documentsService.getDocumentsByLoanNumber(loanNumber),
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
