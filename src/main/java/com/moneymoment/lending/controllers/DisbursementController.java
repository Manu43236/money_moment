package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.DisbursementRequestDto;
import com.moneymoment.lending.dtos.DisbursementResponseDto;
import com.moneymoment.lending.services.DisbursementService;

@RestController
@RequestMapping("/api/disbursements")
public class DisbursementController {

    private final DisbursementService disbursementService;

    public DisbursementController(DisbursementService disbursementService) {
        this.disbursementService = disbursementService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DisbursementResponseDto>>> getAllDisbursements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        disbursementService.getAllDisbursements(page, size),
                        "Disbursements fetched successfully"));
    }

    @PreAuthorize("hasAnyAuthority('OPERATIONS_MANAGER', 'ADMIN')")
    @PostMapping("/emi/scheduleEmis/{loanNumber}")
    public String postMethodName(@PathVariable String loanNumber) {
        String string = disbursementService.scheduleEmis(loanNumber);
        return string;

    }

    @PreAuthorize("hasAnyAuthority('OPERATIONS_MANAGER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<DisbursementResponseDto>> processDisbursement(
            @RequestBody DisbursementRequestDto request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        disbursementService.processDisbursement(request),
                        "Disbursement processed successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/loan/{loanNumber}")
    public ResponseEntity<ApiResponse<DisbursementResponseDto>> getDisbursementByLoan(
            @PathVariable String loanNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        disbursementService.getDisbursementByLoan(loanNumber),
                        "Disbursement details fetched successfully"));
    }
}