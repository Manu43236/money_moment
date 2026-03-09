package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.dtos.CollateralRequestDto;
import com.moneymoment.lending.dtos.CollateralResponseDto;
import com.moneymoment.lending.services.CollateralService;

@RestController
@RequestMapping("/api/collateral")
public class CollateralController {

    private final CollateralService collateralService;

    public CollateralController(CollateralService collateralService) {
        this.collateralService = collateralService;
    }

    @PreAuthorize("hasAnyAuthority('CREDIT_ANALYST', 'RISK_MANAGER', 'OPERATIONS_MANAGER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CollateralResponseDto>> registerCollateral(
            @RequestBody CollateralRequestDto request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        collateralService.registerCollateral(request),
                        "Collateral registered successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/loan/{loanNumber}")
    public ResponseEntity<ApiResponse<CollateralResponseDto>> getCollateralByLoan(
            @PathVariable String loanNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        collateralService.getCollateralByLoan(loanNumber),
                        "Collateral details fetched successfully"));
    }

    @PreAuthorize("hasAnyAuthority('BRANCH_MANAGER', 'REGIONAL_MANAGER', 'CHIEF_CREDIT_OFFICER', 'OPERATIONS_MANAGER', 'ADMIN')")
    @PutMapping("/release/{loanNumber}")
    public ResponseEntity<ApiResponse<CollateralResponseDto>> releaseCollateral(
            @PathVariable String loanNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        collateralService.releaseCollateral(loanNumber),
                        "Collateral released successfully"));
    }
}