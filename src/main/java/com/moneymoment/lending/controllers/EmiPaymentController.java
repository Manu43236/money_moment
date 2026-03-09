package com.moneymoment.lending.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.PaymentRequestDto;
import com.moneymoment.lending.dtos.PaymentResponseDto;
import com.moneymoment.lending.services.EmiPaymentService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/emi-payments")
public class EmiPaymentController {

    private final EmiPaymentService emiPaymentService;

    public EmiPaymentController(EmiPaymentService emiPaymentService) {
        this.emiPaymentService = emiPaymentService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String paymentType,
            @RequestParam(required = false) String loanNumber) {

        return ResponseEntity.ok(ApiResponse.success(
                emiPaymentService.getAll(page, size, dateFrom, dateTo, paymentStatus, paymentMode, paymentType, loanNumber),
                "Payments fetched successfully"));
    }

    @PreAuthorize("hasAnyAuthority('OPERATIONS_MANAGER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDto>> processPayment(
            @RequestBody PaymentRequestDto request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        emiPaymentService.processPayment(request),
                        "Payment processed successfully"));
    }
}