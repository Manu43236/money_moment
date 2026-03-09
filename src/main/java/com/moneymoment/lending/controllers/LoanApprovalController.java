package com.moneymoment.lending.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.dtos.ApprovalRequestDto;
import com.moneymoment.lending.dtos.ApprovalResponseDto;
import com.moneymoment.lending.services.ApprovalService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/loan-approval")
public class LoanApprovalController {

    private final ApprovalService approvalService;

    public LoanApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PreAuthorize("hasAnyAuthority('CREDIT_MANAGER', 'BRANCH_MANAGER', 'REGIONAL_MANAGER', 'CHIEF_CREDIT_OFFICER')")
    @PostMapping()
    public ResponseEntity<ApiResponse<ApprovalResponseDto>> approveLoan(
            @RequestBody ApprovalRequestDto approvalRequestDto) {

        return ResponseEntity.ok(ApiResponse.success(approvalService.approvalProcess(approvalRequestDto),
                "Approval added successfully"));
    }

    // Get approval history
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/history/{loanNumber}")
    public ResponseEntity<ApiResponse<List<ApprovalResponseDto>>> getApprovalHistory(
            @PathVariable String loanNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        approvalService.getApprovalHistory(loanNumber),
                        "Approval history fetched successfully"));
    }

}
