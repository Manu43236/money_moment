package com.moneymoment.lending.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.dtos.LoanRequestDto;
import com.moneymoment.lending.dtos.LoanResponseDto;
import com.moneymoment.lending.dtos.LoanTimelineEventDto;
import com.moneymoment.lending.services.LoanService;
import com.moneymoment.lending.services.LoanTimelineService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanTimelineService loanTimelineService;

    LoanController(LoanService loanService, LoanTimelineService loanTimelineService) {
        this.loanService = loanService;
        this.loanTimelineService = loanTimelineService;
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<LoanResponseDto>> postMethodName(@RequestBody LoanRequestDto loanRequestDto) {
        System.out.println("Received loan creation request: " + loanRequestDto);
        return ResponseEntity
                .ok(ApiResponse.success(loanService.createLoan(loanRequestDto), "Successfully created loan"));
    }

    // get all loans
    @GetMapping()
    public ResponseEntity<ApiResponse<List<LoanResponseDto>>> getMethodName() {
        return ResponseEntity.ok(ApiResponse.success(loanService.fetchAllLoans(), "Successfully fetched loans"));
    }

    // get loan by id
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanResponseDto>> getLoanById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(loanService.fetchLoanById(id), "Successfully fetched loan"));
    }

    // get loan by loan number
    @GetMapping("/loan-number/{loanNumber}")
    public ResponseEntity<ApiResponse<LoanResponseDto>> getLoanByLoanNumber(@PathVariable String loanNumber) {
        return ResponseEntity
                .ok(ApiResponse.success(loanService.fetchLoanByLoanNumber(loanNumber), "Successfully fetched loan"));
    }

    // get loans by customer id
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<LoanResponseDto>>> getLoansByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.fetchLoansByCustomerId(customerId),
                "Successfully fetched loans for customer"));
    }

    // get loan timeline
    @GetMapping("/{loanNumber}/timeline")
    public ResponseEntity<ApiResponse<List<LoanTimelineEventDto>>> getLoanTimeline(@PathVariable String loanNumber) {
        return ResponseEntity.ok(ApiResponse.success(loanTimelineService.getTimeline(loanNumber),
                "Timeline fetched successfully"));
    }

}
