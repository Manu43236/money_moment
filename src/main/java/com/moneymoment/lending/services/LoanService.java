package com.moneymoment.lending.services;

import com.moneymoment.lending.master.MasterService;
import com.moneymoment.lending.master.repos.LoanPurposesRepo;
import com.moneymoment.lending.master.repos.LoanStatusesRepo;
import com.moneymoment.lending.master.repos.LoneTypeRepo;
import com.moneymoment.lending.repos.CustomerRepository;
import com.moneymoment.lending.repos.LoanRepo;

import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.common.spec.LoanSpecification;

import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.enums.LoanStatusEnums;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.utils.EmiCalculator;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.common.utils.ProcessingChargeUtils;
import com.moneymoment.lending.dtos.LoanRequestDto;
import com.moneymoment.lending.dtos.LoanResponseDto;
import com.moneymoment.lending.entities.LoanEntity;

@Service
public class LoanService {

        private final MasterService masterService;

        private final LoanStatusesRepo loanStatusesRepo;

        private final LoanPurposesRepo loanPurposesRepo;

        private final LoneTypeRepo loneTypeRepo;

        private final CustomerRepository customerRepository;

        private final LoanRepo loanRepo;

        LoanService(LoanRepo loanRepo, CustomerRepository customerRepository, LoneTypeRepo loneTypeRepo,
                        LoanPurposesRepo loanPurposesRepo, LoanStatusesRepo loanStatusesRepo,
                        MasterService masterService) {
                this.loanRepo = loanRepo;
                this.customerRepository = customerRepository;
                this.loneTypeRepo = loneTypeRepo;
                this.loanPurposesRepo = loanPurposesRepo;
                this.loanStatusesRepo = loanStatusesRepo;
                this.masterService = masterService;
        }

        @Transactional
        public LoanResponseDto createLoan(LoanRequestDto loanRequestDto) {
                // Validate and fetch related entities
                System.out.println("Step 1: Fetching customer");
                var customer = customerRepository.findById(loanRequestDto.getCustomerId())
                                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id",
                                                loanRequestDto.getCustomerId()));
                System.out.println("Customer fetched: " + customer.getId());

                System.out.println("Step 2: Fetching loan type");
                var loanType = loneTypeRepo.findByCode(loanRequestDto.getLoanTypeCode())
                                .orElseThrow(() -> new ResourceNotFoundException("Loan type ", "code",
                                                loanRequestDto.getLoanTypeCode()));
                System.out.println("Loan type fetched: " + loanType.getId());

                System.out.println("Step 3: Fetching loan purpose");
                var loanPurpose = loanPurposesRepo.findByCode(loanRequestDto.getLoanPurposeCode())
                                .orElseThrow(() -> new ResourceNotFoundException("Loan purpose", "code",
                                                loanRequestDto.getLoanPurposeCode()));
                System.out.println("Loan purpose fetched: " + loanPurpose.getId());

                System.out.println("Step 4: Fetching loan status");
                var loanStatus = loanStatusesRepo.findByCode(LoanStatusEnums.INITIATED)
                                .orElseThrow(() -> new ResourceNotFoundException("Default loan status", "code",
                                                LoanStatusEnums.INITIATED));
                System.out.println("Loan status fetched: " + loanStatus.getId());

                System.out.println("Step 5: Getting interest rate");
                var rateOfIntrest = masterService.getApplicableInterestRate(loanType.getCode(),
                                customer.getCreditScore(),
                                loanRequestDto.getLoanAmount(), loanRequestDto.getTenureMonths());
                System.out.println("Interest rate: " + rateOfIntrest);

                System.out.println("Step 6: Getting processing fee config");
                var processingFee = ProcessingChargeUtils.calculateProcessingFee(
                                masterService.getProcessingFeeConfig(loanType.getCode()),
                                loanRequestDto.getLoanAmount());
                System.out.println("Processing fee: " + processingFee);

                var emi = EmiCalculator.calculateEmi(loanRequestDto.getLoanAmount(), rateOfIntrest,
                                loanRequestDto.getTenureMonths());

                double totalInterest = EmiCalculator.calculateTotalInterest(loanRequestDto.getLoanAmount(), emi,
                                loanRequestDto.getTenureMonths());

                var totalAmount = EmiCalculator.calculateTotalAmount(loanRequestDto.getLoanAmount(), emi,
                                loanRequestDto.getTenureMonths());

                var outstandingAmount = totalAmount;

                LoanEntity loanEntity = new LoanEntity();
                loanEntity.setCustomer(customer);
                loanEntity.setLoanType(loanType);
                loanEntity.setLoanPurpose(loanPurpose);
                loanEntity.setLoanStatus(loanStatus);

                // Set loan details
                loanEntity.setLoanAmount(loanRequestDto.getLoanAmount());
                loanEntity.setTenureMonths(loanRequestDto.getTenureMonths());
                loanEntity.setPurpose(loanRequestDto.getPurpose());
                loanEntity.setDisbursementAccountNumber(loanRequestDto.getDisbursementAccountNumber());
                loanEntity.setDisbursementIfsc(loanRequestDto.getDisbursementIfsc());

                // caliculate and set financial details
                loanEntity.setInterestRate(rateOfIntrest);
                loanEntity.setProcessingFee(processingFee);
                loanEntity.setEmiAmount(emi);
                loanEntity.setTotalInterest(totalInterest);
                loanEntity.setTotalAmount(totalAmount);
                loanEntity.setOutstandingAmount(outstandingAmount);

                // generate loan number and set audit fields
                loanEntity.setLoanNumber(NumberGenerator.generateLoanNumber());
                loanEntity.setAppliedDate(LocalDateTime.now());

                loanEntity.setRepaymentFrequency("MONTHLY");

                System.out.println("Saving loan entity: " + loanEntity);

                loanEntity = loanRepo.save(loanEntity);

                return toDto(loanEntity);
        }

        private LoanResponseDto toDto(LoanEntity loan) {
                LoanResponseDto dto = new LoanResponseDto();

                // Identifiers
                dto.setId(loan.getId());
                dto.setLoanNumber(loan.getLoanNumber());

                // Customer Info
                dto.setCustomerId(loan.getCustomer().getId());
                dto.setCustomerName(loan.getCustomer().getName());
                dto.setCustomerNumber(loan.getCustomer().getCustomerNumber());

                // Loan Type & Purpose
                dto.setLoanTypeCode(loan.getLoanType().getCode());
                dto.setLoanTypeName(loan.getLoanType().getName());
                dto.setLoanPurposeCode(loan.getLoanPurpose().getCode());
                dto.setLoanPurposeName(loan.getLoanPurpose().getName());

                // Loan Details
                dto.setLoanAmount(loan.getLoanAmount());
                dto.setInterestRate(loan.getInterestRate());
                dto.setTenureMonths(loan.getTenureMonths());
                dto.setPurpose(loan.getPurpose());

                // Financial
                dto.setProcessingFee(loan.getProcessingFee());
                dto.setEmiAmount(loan.getEmiAmount());
                dto.setTotalInterest(loan.getTotalInterest());
                dto.setTotalAmount(loan.getTotalAmount());
                dto.setOutstandingAmount(loan.getOutstandingAmount());

                // Status
                dto.setLoanStatusCode(loan.getLoanStatus().getCode());
                dto.setLoanStatusName(loan.getLoanStatus().getName());

                // Dates
                dto.setAppliedDate(loan.getAppliedDate());
                dto.setApprovedDate(loan.getApprovedDate());
                dto.setDisbursedDate(loan.getDisbursedDate());
                dto.setClosedDate(loan.getClosedDate());
                dto.setRejectedDate(loan.getRejectedDate());

                // Rejection
                dto.setRejectionReason(loan.getRejectionReason());

                // Disbursement
                if (loan.getDisbursementMode() != null) {
                        dto.setDisbursementModeCode(loan.getDisbursementMode().getCode());
                        dto.setDisbursementModeName(loan.getDisbursementMode().getName());
                }
                dto.setDisbursementAccountNumber(loan.getDisbursementAccountNumber());
                dto.setDisbursementIfsc(loan.getDisbursementIfsc());

                // Audit
                dto.setCreatedAt(loan.getCreatedAt());
                dto.setUpdatedAt(loan.getUpdatedAt());

                return dto;
        }

        // get all loans - for admin dashboard
        @Transactional
        public PagedResponse<LoanResponseDto> fetchAllLoans(int page, int size, String status, Long customerId, String loanTypeCode) {
                Specification<LoanEntity> spec = Specification.allOf(
                                LoanSpecification.hasStatus(status),
                                LoanSpecification.hasCustomerId(customerId),
                                LoanSpecification.hasLoanTypeCode(loanTypeCode));

                var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                return PagedResponse.of(loanRepo.findAll(spec, pageable).map(this::toDto));
        }

        // get loans by customer id - for customer dashboard
        @Transactional
        public PagedResponse<LoanResponseDto> fetchLoansByCustomerId(Long customerId, int page, int size) {
                customerRepository.findById(customerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

                var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                return PagedResponse.of(loanRepo.findByCustomerId(customerId, pageable).map(this::toDto));
        }

        // get loan by loan number - for detailed loan view
        @Transactional
        public LoanResponseDto fetchLoanByLoanNumber(String loanNumber) {
                return loanRepo.findByLoanNumber(loanNumber)
                                .map(loan -> toDto(loan))
                                .orElseThrow(() -> new ResourceNotFoundException("Loan Number", "loanNumber",
                                                loanNumber));
        }

        // get loan by id - for internal use
        @Transactional
        public LoanResponseDto fetchLoanById(Long id) {
                return loanRepo.findById(id)
                                .map(loan -> toDto(loan))
                                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", id));
        }

}
