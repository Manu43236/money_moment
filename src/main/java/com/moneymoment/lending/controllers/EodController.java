package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.EodLogResponseDto;
import com.moneymoment.lending.dtos.EodResultDto;
import com.moneymoment.lending.services.EodService;

@RestController
@RequestMapping("/api/eod")
public class EodController {

    private final EodService eodService;

    public EodController(EodService eodService) {
        this.eodService = eodService;
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<EodLogResponseDto>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(eodService.getHistory(page, size), "EOD history fetched"));
    }

    @PostMapping("/run-now")
    public ResponseEntity<ApiResponse<EodResultDto>> runEodManually() {
        try {
            EodResultDto result = eodService.processEod();
            return ResponseEntity.ok(ApiResponse.success(result, "EOD executed successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("EOD processing failed: " + e.getMessage()));
        }
    }
}