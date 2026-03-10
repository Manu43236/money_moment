package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.EodJobStatus;
import com.moneymoment.lending.dtos.EodLogResponseDto;
import com.moneymoment.lending.services.EodAsyncExecutor;
import com.moneymoment.lending.services.EodService;

@RestController
@RequestMapping("/api/eod")
public class EodController {

    private final EodService eodService;
    private final EodAsyncExecutor eodAsyncExecutor;

    public EodController(EodService eodService, EodAsyncExecutor eodAsyncExecutor) {
        this.eodService = eodService;
        this.eodAsyncExecutor = eodAsyncExecutor;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/run-now")
    public ResponseEntity<ApiResponse<EodJobStatus>> runEodManually() {
        if (eodService.isRunning()) {
            return ResponseEntity.ok(ApiResponse.error("EOD is already running"));
        }
        eodAsyncExecutor.run("MANUAL");
        return ResponseEntity.ok(ApiResponse.success(eodService.getStatus(), "EOD started in background"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<EodJobStatus>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success(eodService.getStatus(), "EOD status fetched"));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'BRANCH_MANAGER', 'REGIONAL_MANAGER', 'CHIEF_CREDIT_OFFICER')")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<EodLogResponseDto>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(eodService.getHistory(page, size), "EOD history fetched"));
    }
}
