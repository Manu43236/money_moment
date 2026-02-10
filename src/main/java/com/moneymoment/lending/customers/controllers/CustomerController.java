package com.moneymoment.lending.customers.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.customers.dtos.CustomerRequestDto;
import com.moneymoment.lending.customers.dtos.CustomerResponseDto;
import com.moneymoment.lending.customers.services.CustomerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponseDto>>> fetchAllUsers() {
        return ResponseEntity
                .ok(ApiResponse.success(customerService.fetchAllUsers(), "Successfully fetched all customers"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDto>> createCustomer(@RequestBody CustomerRequestDto entity) {
        // TODO: process POST request

        return ResponseEntity
                .ok(ApiResponse.success(customerService.createCustomer(entity), "Successfully created customer"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> updateCustomer(@PathVariable Long id,
            @RequestBody CustomerRequestDto entity) {
        // TODO: process PUT request

        return ResponseEntity
                .ok(ApiResponse.success(customerService.updateCustomer(id, entity), "Successfully updated customer"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> fetchCustomerById(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success(customerService.fetchCustomerById(id), "Successfully fetched customer"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Successfully deleted customer"));
    }
}
