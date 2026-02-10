package com.moneymoment.lending.customers.services;

import com.moneymoment.lending.common.exception.DuplicateRecordException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.common.validation.AadhaarValidator;
import com.moneymoment.lending.common.validation.PanValidator;
import com.moneymoment.lending.common.validation.PhoneValidator;
import com.moneymoment.lending.customers.dtos.CustomerRequestDto;
import com.moneymoment.lending.customers.dtos.CustomerResponseDto;
import com.moneymoment.lending.customers.entities.CustomerEntity;
import com.moneymoment.lending.customers.repos.CustomerRepository;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository; // Constructor for dependency injection if needed
    }

    public CustomerResponseDto fetchCustomerById(Long id) {
        return customerRepository.findById(id)
                .map(entity -> toDto(entity))
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    public CustomerResponseDto createCustomer(CustomerRequestDto custRespDto) {

        // Validate inputs
        PanValidator.validate(custRespDto.getPan());
        AadhaarValidator.validate(custRespDto.getAadhar());
        PhoneValidator.validate(custRespDto.getPhone());

        // Check for duplicates
        if (customerRepository.findByEmail(custRespDto.getEmail()).isPresent()) {
            throw new DuplicateRecordException("Customer already exists with email: " + custRespDto.getEmail());
        }

        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setName(custRespDto.getName());
        customerEntity.setPhone(custRespDto.getPhone());
        customerEntity.setDob(custRespDto.getDob());
        customerEntity.setEmail(custRespDto.getEmail());
        customerEntity.setPan(custRespDto.getPan());
        customerEntity.setAadhar(custRespDto.getAadhar());
        customerEntity.setAddress(custRespDto.getAddress());
        customerEntity.setOccupation(custRespDto.getOccupation());
        customerEntity.setEmploymentType(custRespDto.getEmploymentType());
        customerEntity.setMonthlySalary(custRespDto.getMonthlySalary());

        customerEntity.setCustomerNumber(NumberGenerator.generateCustomerNumber());

        customerEntity = customerRepository.save(customerEntity);
        return toDto(customerEntity);

    }

    public List<CustomerResponseDto> fetchAllUsers() {
        return customerRepository.findAll().stream()
                .map(entity -> toDto(entity))
                .toList();
    }

    public CustomerResponseDto updateCustomer(Long id, CustomerRequestDto custRespDto) {
        return customerRepository.findById(id)
                .map(entity -> {
                    entity.setName(custRespDto.getName());
                    entity.setPhone(custRespDto.getPhone());
                    entity.setDob(custRespDto.getDob());
                    entity.setEmail(custRespDto.getEmail());
                    entity.setPan(custRespDto.getPan());
                    entity.setAadhar(custRespDto.getAadhar());
                    entity.setAddress(custRespDto.getAddress());
                    entity.setOccupation(custRespDto.getOccupation());
                    entity.setEmploymentType(custRespDto.getEmploymentType());
                    entity.setMonthlySalary(custRespDto.getMonthlySalary());

                    return customerRepository.save(entity);
                })
                .map(updatedEntity -> toDto(updatedEntity))
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
        // Implementation for deleting a customer by ID
    }

    private CustomerResponseDto toDto(CustomerEntity entity) {
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPhone(entity.getPhone());
        dto.setDob(entity.getDob());
        dto.setEmail(entity.getEmail());
        dto.setPan(entity.getPan());
        dto.setAadhar(entity.getAadhar());
        dto.setAddress(entity.getAddress());
        dto.setOccupation(entity.getOccupation());
        dto.setEmploymentType(entity.getEmploymentType());
        dto.setMonthlySalary(entity.getMonthlySalary());
        dto.setCustomerNumber(entity.getCustomerNumber());

        return dto;
    }
}
