package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.dtos.PreClosureRequestDto;
import com.moneymoment.lending.services.LoanClosureService;
import com.moneymoment.lending.services.LoanClosureService.LoanClosureSummary;
import com.moneymoment.lending.services.LoanClosureService.PreClosureQuote;
import com.moneymoment.lending.services.LoanClosureService.PreClosureSummaryFull;

@RestController
@RequestMapping("/api/loans")
public class LoanClosureController {

    private final LoanClosureService loanClosureService;

    public LoanClosureController(LoanClosureService loanClosureService) {
        this.loanClosureService = loanClosureService;
    }

    @PreAuthorize("hasAnyAuthority('BRANCH_MANAGER', 'REGIONAL_MANAGER', 'CHIEF_CREDIT_OFFICER', 'ADMIN')")
    @PostMapping("/{loanNumber}/close")
    public ResponseEntity<ApiResponse<LoanClosureSummary>> closeLoan(
            @PathVariable String loanNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                loanClosureService.closeLoan(loanNumber),
                "Loan closed successfully"));
    }

    @PreAuthorize("hasAnyAuthority('OPERATIONS_MANAGER', 'BRANCH_MANAGER', 'REGIONAL_MANAGER', 'CHIEF_CREDIT_OFFICER', 'ADMIN')")
    @GetMapping("/{loanNumber}/pre-closure-quote")
    public ResponseEntity<ApiResponse<PreClosureQuote>> getPreClosureQuote(
            @PathVariable String loanNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                loanClosureService.getPreClosureQuote(loanNumber),
                "Pre-closure quote fetched"));
    }

    @PreAuthorize("hasAnyAuthority('OPERATIONS_MANAGER', 'BRANCH_MANAGER', 'REGIONAL_MANAGER', 'CHIEF_CREDIT_OFFICER', 'ADMIN')")
    @PostMapping("/{loanNumber}/pre-close")
    public ResponseEntity<ApiResponse<PreClosureSummaryFull>> preCloseLoan(
            @PathVariable String loanNumber,
            @RequestBody PreClosureRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                loanClosureService.preCloseLoan(loanNumber, request),
                "Loan pre-closed successfully"));
    }
}