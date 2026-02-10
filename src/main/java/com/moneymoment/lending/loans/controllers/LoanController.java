package com.moneymoment.lending.loans.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.loans.dto.LoanRequestDto;
import com.moneymoment.lending.loans.dto.LoanResponseDto;
import com.moneymoment.lending.loans.services.LoanService;

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

    LoanController(LoanService loanService) {
        this.loanService = loanService;
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

}
