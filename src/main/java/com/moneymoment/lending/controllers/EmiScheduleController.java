package com.moneymoment.lending.controllers;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.EmiScheduleResponseDto;
import com.moneymoment.lending.services.EmiScheduleService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/emi-schedules")
public class EmiScheduleController {

    private final EmiScheduleService emiScheduleService;

    EmiScheduleController(EmiScheduleService emiScheduleService) {
        this.emiScheduleService = emiScheduleService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EmiScheduleResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(required = false) Integer dpdMin,
            @RequestParam(required = false) Integer dpdMax,
            @RequestParam(required = false) String loanNumber) {

        return ResponseEntity.ok(ApiResponse.success(
                emiScheduleService.fetchAll(page, size, status, dueDateFrom, dueDateTo, dpdMin, dpdMax, loanNumber),
                "EMI schedules fetched successfully"));
    }
}
