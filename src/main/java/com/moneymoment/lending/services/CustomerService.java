package com.moneymoment.lending.services;

import com.moneymoment.lending.common.exception.DuplicateRecordException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.common.validation.AadhaarValidator;
import com.moneymoment.lending.common.validation.PanValidator;
import com.moneymoment.lending.common.validation.PhoneValidator;
import com.moneymoment.lending.dtos.CustomerRequestDto;
import com.moneymoment.lending.dtos.CustomerResponseDto;
import com.moneymoment.lending.entities.CustomerEntity;
import com.moneymoment.lending.entities.UserEntity;
import com.moneymoment.lending.repos.CustomerRepository;
import com.moneymoment.lending.repos.UserRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto fetchCustomerById(Long id) {
        return customerRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    @Transactional
    public CustomerResponseDto createCustomer(CustomerRequestDto request) {

        PanValidator.validate(request.getPan());
        AadhaarValidator.validate(request.getAadhar());
        PhoneValidator.validate(request.getPhone());

        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateRecordException("Customer already exists with email: " + request.getEmail());
        }

        UserEntity relationshipManager = null;
        if (request.getRelationshipManagerEmployeeId() != null
                && !request.getRelationshipManagerEmployeeId().isEmpty()) {
            relationshipManager = userRepository.findByEmployeeId(request.getRelationshipManagerEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "employeeId",
                            request.getRelationshipManagerEmployeeId()));
        }

        CustomerEntity customer = new CustomerEntity();
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setDob(request.getDob());
        customer.setEmail(request.getEmail());
        customer.setPan(request.getPan());
        customer.setAadhar(request.getAadhar());
        customer.setAddress(request.getAddress());
        customer.setOccupation(request.getOccupation());
        customer.setEmploymentType(request.getEmploymentType());
        customer.setMonthlySalary(request.getMonthlySalary());
        customer.setHomeBranchCode(request.getHomeBranchCode());
        customer.setRelationshipManager(relationshipManager);
        customer.setCreatedBy(request.getCreatedBy());
        customer.setCustomerNumber(NumberGenerator.generateCustomerNumber());

        customer = customerRepository.save(customer);
        return toDto(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponseDto> fetchAllUsers() {
        return customerRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CustomerResponseDto updateCustomer(Long id, CustomerRequestDto request) {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setDob(request.getDob());
        customer.setEmail(request.getEmail());
        customer.setPan(request.getPan());
        customer.setAadhar(request.getAadhar());
        customer.setAddress(request.getAddress());
        customer.setOccupation(request.getOccupation());
        customer.setEmploymentType(request.getEmploymentType());
        customer.setMonthlySalary(request.getMonthlySalary());

        return toDto(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    private CustomerResponseDto toDto(CustomerEntity entity) {
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(entity.getId());
        dto.setCustomerNumber(entity.getCustomerNumber());
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
        dto.setHomeBranchCode(entity.getHomeBranchCode());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getRelationshipManager() != null) {
            dto.setRelationshipManagerEmployeeId(entity.getRelationshipManager().getEmployeeId());
            dto.setRelationshipManagerName(entity.getRelationshipManager().getFullName());
        }

        return dto;
    }
}
