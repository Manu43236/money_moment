package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.services.EodService;

@RestController
@RequestMapping("/api/eod")
public class EodController {

    private final EodService eodService;

    public EodController(EodService eodService) {
        this.eodService = eodService;
    }

    @PostMapping("/run-now")
    public ResponseEntity<ApiResponse<String>> runEodManually() {

        try {
            eodService.processEod();
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "EOD processing completed successfully",
                            "EOD executed manually"));
        } catch (Exception e) {
            return ResponseEntity.ok(
                    ApiResponse.error(
                            "EOD processing failed: " + e.getMessage()));
        }
    }
}