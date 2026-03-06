package com.moneymoment.lending.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.response.PagedResponse;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.enums.LoanStatusEnums;
import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.dtos.DisbursementRequestDto;
import com.moneymoment.lending.dtos.DisbursementResponseDto;
import com.moneymoment.lending.dtos.EmiScheduleResponseDto;
import com.moneymoment.lending.entities.DisbursementEntity;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.entities.UserEntity;
import com.moneymoment.lending.master.entities.LoanStatusesEntity;
import com.moneymoment.lending.master.repos.LoanStatusesRepo;
import com.moneymoment.lending.repos.CollateralDetailsRepository;
import com.moneymoment.lending.repos.DisbursementRepository;
import com.moneymoment.lending.repos.LoanRepo;
import com.moneymoment.lending.repos.UserRepository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
public class DisbursementService {

    private final DisbursementRepository disbursementRepository;
    private final LoanRepo loanRepo;
    private final UserRepository userRepository;
    private final LoanStatusesRepo loanStatusesRepo;
    private final CollateralDetailsRepository collateralDetailsRepository;
    private EMIScheduleGenerationService emiScheduleGenerationService;

    public DisbursementService(DisbursementRepository disbursementRepository, LoanRepo loanRepo,
            UserRepository userRepository, LoanStatusesRepo loanStatusesRepo,
            CollateralDetailsRepository collateralDetailsRepository,
            EMIScheduleGenerationService emiScheduleGenerationService) {
        this.disbursementRepository = disbursementRepository;
        this.loanRepo = loanRepo;
        this.userRepository = userRepository;
        this.loanStatusesRepo = loanStatusesRepo;
        this.collateralDetailsRepository = collateralDetailsRepository;
        this.emiScheduleGenerationService = emiScheduleGenerationService;
    }

    @Transactional
    public String scheduleEmis(String loannumber) {

        LoanEntity loan = loanRepo.findByLoanNumber(loannumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loannumber));

        List<EmiScheduleResponseDto> emiScheduleResponseDtos = emiScheduleGenerationService
                .generateSchedule(loan.getId());

        return "EMIs scheduled successfully";

    }

    @Transactional
    public DisbursementResponseDto processDisbursement(DisbursementRequestDto request) {

        // Step 1: Fetch Loan
        LoanEntity loan = loanRepo.findByLoanNumber(request.getLoanNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", request.getLoanNumber()));

        // Step 2: Validate Loan Status
        if (!loan.getLoanStatus().getCode().equals(LoanStatusEnums.APPROVED)) {
            throw new BusinessLogicException(
                    "Loan is not approved for disbursement. Current status: " + loan.getLoanStatus().getCode());
        }

        // Step 3: Check if Already Disbursed
        Optional<DisbursementEntity> existingDisbursement = disbursementRepository.findByLoanId(loan.getId());

        if (existingDisbursement.isPresent()) {
            throw new BusinessLogicException(
                    "Loan already disbursed. Disbursement number: "
                            + existingDisbursement.get().getDisbursementNumber());
        }

        // Step 4: For secured loans, collateral must be registered before disbursement
        if (loan.getLoanType().getCollateralRequired()) {
            boolean collateralExists = collateralDetailsRepository.findByLoanId(loan.getId()).isPresent();
            if (!collateralExists) {
                throw new BusinessLogicException(
                        "Collateral must be registered before disbursing a secured loan. "
                                + "Please register collateral for loan: " + loan.getLoanNumber());
            }
        }

        // Step 5: Fetch Employee
        UserEntity disbursedBy = userRepository.findByEmployeeId(request.getDisbursedByEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "employeeId",
                        request.getDisbursedByEmployeeId()));

        // Step 5: Calculate Net Disbursement
        Double disbursementAmount = loan.getLoanAmount();
        Double processingFee = loan.getProcessingFee();
        Double netDisbursement = disbursementAmount; // Full amount (not deducting fee)

        // Step 6: Create Disbursement Entity
        DisbursementEntity disbursement = new DisbursementEntity();

        disbursement.setDisbursementNumber(
                NumberGenerator.numberGeneratorWithPrifix(AppConstants.DISBURSEMENT_NUMBER_PREFIX));

        disbursement.setLoan(loan);
        disbursement.setCustomer(loan.getCustomer());
        disbursement.setDisbursedByUser(disbursedBy);

        disbursement.setDisbursementAmount(disbursementAmount);
        disbursement.setProcessingFee(processingFee);
        disbursement.setNetDisbursement(netDisbursement);

        disbursement.setBeneficiaryAccountNumber(loan.getDisbursementAccountNumber());
        disbursement.setBeneficiaryIfsc(loan.getDisbursementIfsc());
        disbursement.setBeneficiaryName(loan.getCustomer().getName());

        disbursement.setDisbursementMode(request.getDisbursementMode());
        disbursement.setStatus("INITIATED");
        disbursement.setInitiatedAt(LocalDateTime.now());

        disbursement.setDisbursedByEmployeeId(disbursedBy.getEmployeeId());
        disbursement.setDisbursedByName(disbursedBy.getFullName());

        // Step 7: Mock Payment Gateway
        PaymentGatewayResponse gatewayResponse = mockPaymentGateway(
                netDisbursement,
                loan.getDisbursementAccountNumber(),
                loan.getDisbursementIfsc(),
                loan.getCustomer().getName());

        // Step 8: Update Based on Response
        if (gatewayResponse.getStatus().equals("SUCCESS")) {
            disbursement.setStatus("SUCCESS");
            disbursement.setTransactionId(gatewayResponse.getTransactionId());
            disbursement.setUtrNumber(gatewayResponse.getUtrNumber());
            disbursement.setCompletedAt(LocalDateTime.now());
        } else {
            disbursement.setStatus("FAILED");
            disbursement.setFailureReason(gatewayResponse.getFailureReason());
        }

        // Step 9: Save Disbursement
        disbursement = disbursementRepository.save(disbursement);

        // Step 10: Update Loan Status (only if SUCCESS)
        if (gatewayResponse.getStatus().equals("SUCCESS")) {
            LoanStatusesEntity disbursedStatus = loanStatusesRepo.findByCode(LoanStatusEnums.DISBURSED)
                    .orElseThrow(() -> new ResourceNotFoundException("LoanStatus", "code", LoanStatusEnums.DISBURSED));

            loan.setLoanStatus(disbursedStatus);
            loan.setDisbursedDate(LocalDateTime.now());
            loanRepo.save(loan);

            // add emis
            List<EmiScheduleResponseDto> emiScheduleResponseDtos = emiScheduleGenerationService
                    .generateSchedule(loan.getId());

        }

        // Step 11: Return Response
        return toDto(disbursement);
    }

    // Mock Payment Gateway
    private PaymentGatewayResponse mockPaymentGateway(Double amount, String account, String ifsc, String name) {
        // Simulate 90% success rate
        boolean success = Math.random() > 0.1;

        if (success) {
            String transactionId = "TXN" + System.currentTimeMillis();
            String utrNumber = "UTR" + System.currentTimeMillis();
            return new PaymentGatewayResponse("SUCCESS", transactionId, utrNumber, null);
        } else {
            return new PaymentGatewayResponse("FAILED", null, null, "Invalid account number or IFSC code");
        }
    }

    // Get All Disbursements (paginated)
    public PagedResponse<DisbursementResponseDto> getAllDisbursements(int page, int size) {
        Page<DisbursementResponseDto> dtoPage = disbursementRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toDto);
        return PagedResponse.of(dtoPage);
    }

    // Get Disbursement by Loan
    @Transactional
    public DisbursementResponseDto getDisbursementByLoan(String loanNumber) {
        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        DisbursementEntity disbursement = disbursementRepository.findByLoanId(loan.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Disbursement not found for loan: " + loanNumber));

        return toDto(disbursement);
    }

    // toDto
    private DisbursementResponseDto toDto(DisbursementEntity entity) {
        DisbursementResponseDto dto = new DisbursementResponseDto();

        dto.setId(entity.getId());
        dto.setDisbursementNumber(entity.getDisbursementNumber());

        dto.setLoanId(entity.getLoan().getId());
        dto.setLoanNumber(entity.getLoan().getLoanNumber());

        dto.setCustomerId(entity.getCustomer().getId());
        dto.setCustomerNumber(entity.getCustomer().getCustomerNumber());
        dto.setCustomerName(entity.getCustomer().getName());

        dto.setDisbursementAmount(entity.getDisbursementAmount());
        dto.setProcessingFee(entity.getProcessingFee());
        dto.setNetDisbursement(entity.getNetDisbursement());

        dto.setBeneficiaryAccountNumber(entity.getBeneficiaryAccountNumber());
        dto.setBeneficiaryIfsc(entity.getBeneficiaryIfsc());
        dto.setBeneficiaryName(entity.getBeneficiaryName());

        dto.setDisbursementMode(entity.getDisbursementMode());
        dto.setTransactionId(entity.getTransactionId());
        dto.setUtrNumber(entity.getUtrNumber());

        dto.setStatus(entity.getStatus());
        dto.setFailureReason(entity.getFailureReason());

        dto.setInitiatedAt(entity.getInitiatedAt());
        dto.setCompletedAt(entity.getCompletedAt());

        dto.setDisbursedByEmployeeId(entity.getDisbursedByEmployeeId());
        dto.setDisbursedByName(entity.getDisbursedByName());

        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }

    // Inner class for Payment Gateway Response
    @Data
    @AllArgsConstructor
    private static class PaymentGatewayResponse {
        private String status;
        private String transactionId;
        private String utrNumber;
        private String failureReason;
    }
}