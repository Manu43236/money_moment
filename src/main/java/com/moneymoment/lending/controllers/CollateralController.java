package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<ApiResponse<CollateralResponseDto>> registerCollateral(
            @RequestBody CollateralRequestDto request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        collateralService.registerCollateral(request),
                        "Collateral registered successfully"));
    }

    @GetMapping("/loan/{loanNumber}")
    public ResponseEntity<ApiResponse<CollateralResponseDto>> getCollateralByLoan(
            @PathVariable String loanNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        collateralService.getCollateralByLoan(loanNumber),
                        "Collateral details fetched successfully"));
    }

    @PutMapping("/release/{loanNumber}")
    public ResponseEntity<ApiResponse<CollateralResponseDto>> releaseCollateral(
            @PathVariable String loanNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        collateralService.releaseCollateral(loanNumber),
                        "Collateral released successfully"));
    }
}