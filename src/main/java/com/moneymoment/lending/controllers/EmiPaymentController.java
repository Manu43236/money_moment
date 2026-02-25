package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.dtos.PaymentRequestDto;
import com.moneymoment.lending.dtos.PaymentResponseDto;
import com.moneymoment.lending.services.EmiPaymentService;

@RestController
@RequestMapping("/api/emi-payments")
public class EmiPaymentController {

    private final EmiPaymentService emiPaymentService;

    public EmiPaymentController(EmiPaymentService emiPaymentService) {
        this.emiPaymentService = emiPaymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDto>> processPayment(
            @RequestBody PaymentRequestDto request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        emiPaymentService.processPayment(request),
                        "Payment processed successfully"));
    }
}