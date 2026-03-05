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
import com.moneymoment.lending.repos.LoanRepo;
import com.moneymoment.lending.repos.UserRepository;

import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.common.spec.CustomerSpecification;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private static final List<String> INACTIVE_STATUSES = List.of("CLOSED", "REJECTED");
    private static final List<String> OVERDUE_STATUSES  = List.of("OVERDUE", "NPA");

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final LoanRepo loanRepo;

    CustomerService(CustomerRepository customerRepository, UserRepository userRepository, LoanRepo loanRepo) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.loanRepo = loanRepo;
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
    public PagedResponse<CustomerResponseDto> fetchAllUsers(int page, int size, String name, String employmentType, String email) {
        Specification<CustomerEntity> spec = Specification.allOf(
                CustomerSpecification.isActive(),
                CustomerSpecification.nameLike(name),
                CustomerSpecification.hasEmploymentType(employmentType),
                CustomerSpecification.emailLike(email));

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.of(customerRepository.findAll(spec, pageable).map(this::toDto));
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
    public void deactivateCustomer(Long id) {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        customer.setIsActive(false);
        customer.setDeactivatedAt(java.time.LocalDateTime.now());
        customerRepository.save(customer);
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
        dto.setIsActive(entity.getIsActive());
        dto.setDeactivatedAt(entity.getDeactivatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getRelationshipManager() != null) {
            dto.setRelationshipManagerEmployeeId(entity.getRelationshipManager().getEmployeeId());
            dto.setRelationshipManagerName(entity.getRelationshipManager().getFullName());
        }

        // Loan summary fields
        dto.setCreditScore(entity.getCreditScore());
        dto.setActiveLoanCount(loanRepo.countByCustomer_IdAndLoanStatus_CodeNotIn(entity.getId(), INACTIVE_STATUSES));
        dto.setHasOverdue(loanRepo.existsByCustomer_IdAndLoanStatus_CodeIn(entity.getId(), OVERDUE_STATUSES));

        return dto;
    }
}
