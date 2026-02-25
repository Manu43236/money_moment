package com.moneymoment.lending.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
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

    @PostMapping("/emi/scheduleEmis/{loanNumber}")
    public String postMethodName(@PathVariable String loanNumber) {
        String string = disbursementService.scheduleEmis(loanNumber);
        return string;

    }

    @PostMapping
    public ResponseEntity<ApiResponse<DisbursementResponseDto>> processDisbursement(
            @RequestBody DisbursementRequestDto request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        disbursementService.processDisbursement(request),
                        "Disbursement processed successfully"));
    }

    @GetMapping("/loan/{loanNumber}")
    public ResponseEntity<ApiResponse<DisbursementResponseDto>> getDisbursementByLoan(
            @PathVariable String loanNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        disbursementService.getDisbursementByLoan(loanNumber),
                        "Disbursement details fetched successfully"));
    }
}