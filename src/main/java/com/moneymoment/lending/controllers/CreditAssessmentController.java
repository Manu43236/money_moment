package com.moneymoment.lending.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.dtos.CreditAssessmentRequestDto;
import com.moneymoment.lending.dtos.CreditAssessmentResponseDto;
import com.moneymoment.lending.entities.CreditAssessmentEntity;
import com.moneymoment.lending.services.CreditAssessmentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/credit-assessment")
public class CreditAssessmentController {

    @Autowired
    public CreditAssessmentService creditAssessmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreditAssessmentResponseDto>> postMethodName(
            @RequestBody CreditAssessmentRequestDto creditAssessmentRequestDto) {

        return ResponseEntity.ok(
                ApiResponse.success(creditAssessmentService.createCreditAssessment(creditAssessmentRequestDto),
                        "Credit Assessment Created Successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditAssessmentResponseDto>> getAssessmentById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        creditAssessmentService.getAssessmentById(id),
                        "Assessment fetched successfully"));
    }

    @GetMapping("/loan/{loanNumber}")
    public ResponseEntity<ApiResponse<CreditAssessmentResponseDto>> getLatestAssessmentByLoan(
            @PathVariable String loanNumber) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        creditAssessmentService.getLatestAssessmentByLoan(loanNumber),
                        "Latest assessment fetched successfully"));
    }
}
