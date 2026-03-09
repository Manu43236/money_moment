package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.services.LoanClosureService;
import com.moneymoment.lending.services.LoanClosureService.LoanClosureSummary;

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

        return ResponseEntity.ok(
                ApiResponse.success(
                        loanClosureService.closeLoan(loanNumber),
                        "Loan closed successfully"));
    }
}