package com.moneymoment.lending.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.CustomerRequestDto;
import com.moneymoment.lending.dtos.CustomerResponseDto;
import com.moneymoment.lending.services.CustomerService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CustomerResponseDto>>> fetchAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(ApiResponse.success(
                customerService.fetchAllUsers(page, size, name, employmentType, email),
                "Successfully fetched all customers"));
    }

    @PreAuthorize("hasAnyAuthority('LOAN_OFFICER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDto>> createCustomer(@RequestBody CustomerRequestDto entity) {
        // TODO: process POST request

        return ResponseEntity
                .ok(ApiResponse.success(customerService.createCustomer(entity), "Successfully created customer"));
    }

    @PreAuthorize("hasAnyAuthority('LOAN_OFFICER', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> updateCustomer(@PathVariable Long id,
            @RequestBody CustomerRequestDto entity) {
        // TODO: process PUT request

        return ResponseEntity
                .ok(ApiResponse.success(customerService.updateCustomer(id, entity), "Successfully updated customer"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> fetchCustomerById(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success(customerService.fetchCustomerById(id), "Successfully fetched customer"));
    }

    @PreAuthorize("hasAnyAuthority('BRANCH_MANAGER', 'REGIONAL_MANAGER', 'CHIEF_CREDIT_OFFICER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(@PathVariable Long id) {
        customerService.deactivateCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deactivated successfully"));
    }
}
