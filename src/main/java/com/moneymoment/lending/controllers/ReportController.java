package com.moneymoment.lending.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.dtos.ReportCollectionDto;
import com.moneymoment.lending.dtos.ReportDisbursementDto;
import com.moneymoment.lending.dtos.ReportDpdBucketDto;
import com.moneymoment.lending.dtos.ReportLoanBookDto;
import com.moneymoment.lending.dtos.ReportNpaLoanDto;
import com.moneymoment.lending.services.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/loan-book")
    public ResponseEntity<ApiResponse<List<ReportLoanBookDto>>> getLoanBook() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getLoanBookSummary(), "Loan book summary fetched"));
    }

    @GetMapping("/collection")
    public ResponseEntity<ApiResponse<ReportCollectionDto>> getCollection(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getCollectionReport(from, to), "Collection report fetched"));
    }

    @GetMapping("/dpd-aging")
    public ResponseEntity<ApiResponse<List<ReportDpdBucketDto>>> getDpdAging() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDpdAgingReport(), "DPD aging report fetched"));
    }

    @GetMapping("/npa")
    public ResponseEntity<ApiResponse<List<ReportNpaLoanDto>>> getNpa() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getNpaLoans(), "NPA report fetched"));
    }

    @GetMapping("/disbursement")
    public ResponseEntity<ApiResponse<List<ReportDisbursementDto>>> getDisbursement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDisbursementReport(from, to), "Disbursement report fetched"));
    }
}
