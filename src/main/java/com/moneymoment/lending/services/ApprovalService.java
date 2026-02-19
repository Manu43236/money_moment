package com.moneymoment.lending.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moneymoment.lending.common.enums.LoanStatusEnums;
import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.exception.ValidationException;
import com.moneymoment.lending.dtos.ApprovalRequestDto;
import com.moneymoment.lending.dtos.ApprovalResponseDto;
import com.moneymoment.lending.entities.LoanApprovalEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.entities.UserEntity;
import com.moneymoment.lending.master.entities.LoanStatusesEntity;
import com.moneymoment.lending.master.repos.LoanStatusesRepo;
import com.moneymoment.lending.repos.LoanApprovalRepository;
import com.moneymoment.lending.repos.LoanRepo;
import com.moneymoment.lending.repos.UserRepository;

@Service
public class ApprovalService {

    @Autowired
    private LoanRepo loanRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanApprovalRepository loanApprovalRepository;

    @Autowired
    private LoanStatusesRepo loanStatusesRepo;

    public ApprovalResponseDto approvalProcess(ApprovalRequestDto approvalRequestDto) {

        if (!approvalRequestDto.getAction().equals("APPROVE") && !approvalRequestDto.getAction().equals("REJECT")) {
            throw new ValidationException("Action must be APPROVE or REJECT");
        }

        LoanEntity loan = loanRepo.findByLoanNumber(approvalRequestDto.getLoanNumber())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Loan", "loanNumber", approvalRequestDto.getLoanNumber()));

        // Loan must be in UNDER_REVIEW or MANUAL_REVIEW status
        String currentStatus = loan.getLoanStatus().getCode();
        if (!currentStatus.equals(LoanStatusEnums.UNDER_REVIEW)
                && !currentStatus.equals(LoanStatusEnums.MANUAL_REVIEW)) {
            throw new BusinessLogicException(
                    "Loan is not in valid state for approval. Current status: " + currentStatus);
        }

        UserEntity approver = userRepository.findByEmployeeId(approvalRequestDto.getApprovedByEmployeeId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("User", "employeeId",
                                approvalRequestDto.getApprovedByEmployeeId()));

        int requiredLevel = determineRequiredApprovalLevel(loan.getLoanAmount());

        int approverLevel = getApproverLevel(approver);

        String approverRoleCode = getApproverRoleCode(approver);

        if (approverLevel < requiredLevel) {
            throw new BusinessLogicException(
                    "Insufficient approval authority. Required level: " + requiredLevel
                            + ", Your level: " + approverLevel);
        }

        // Check if this user already approved/rejected this loan
        List<LoanApprovalEntity> existingApprovals = loanApprovalRepository
                .findByLoanIdOrderByCreatedAtAsc(loan.getId());

        boolean alreadyActioned = existingApprovals.stream()
                .anyMatch(a -> a.getApprovedByEmployeeId().equals(approvalRequestDto.getApprovedByEmployeeId()));

        if (alreadyActioned) {
            throw new BusinessLogicException("You have already taken action on this loan");
        }

        LoanApprovalEntity approval = new LoanApprovalEntity();
        approval.setLoan(loan);
        approval.setCustomer(loan.getCustomer());
        approval.setApprovedByUser(approver);
        approval.setApprovedByEmployeeId(approver.getEmployeeId());
        approval.setApprovedByName(approver.getFullName());
        approval.setApprovalLevel(approverLevel);
        approval.setRoleCode(approverRoleCode);
        approval.setAction(approvalRequestDto.getAction());
        approval.setRemarks(approvalRequestDto.getRemarks());
        approval.setLoanAmount(loan.getLoanAmount());
        approval.setActionTakenAt(LocalDateTime.now());

        approval = loanApprovalRepository.save(approval);

        LoanStatusesEntity newStatus;

        if (approvalRequestDto.getAction().equals("REJECT")) {
            // If rejected, mark loan as REJECTED
            newStatus = loanStatusesRepo.findByCode(LoanStatusEnums.REJECTED)
                    .orElseThrow(() -> new ResourceNotFoundException("LoanStatus", "code", LoanStatusEnums.REJECTED));

            loan.setRejectedDate(LocalDateTime.now());

        } else {
            // If approved, mark loan as APPROVED (ready for disbursement)
            newStatus = loanStatusesRepo.findByCode(LoanStatusEnums.APPROVED)
                    .orElseThrow(() -> new ResourceNotFoundException("LoanStatus", "code", LoanStatusEnums.APPROVED));

            loan.setApprovedDate(LocalDateTime.now());
        }

        loan.setLoanStatus(newStatus);
        loanRepo.save(loan);

        return toDto(approval);
    }

    private ApprovalResponseDto toDto(LoanApprovalEntity entity) {
        ApprovalResponseDto dto = new ApprovalResponseDto();

        // Identifiers
        dto.setId(entity.getId());

        // Loan Info
        dto.setLoanId(entity.getLoan().getId());
        dto.setLoanNumber(entity.getLoan().getLoanNumber());
        dto.setLoanAmount(entity.getLoanAmount());

        // Customer Info
        dto.setCustomerId(entity.getCustomer().getId());
        dto.setCustomerNumber(entity.getCustomer().getCustomerNumber());
        dto.setCustomerName(entity.getCustomer().getName());

        // Approver Info
        dto.setApprovedByUserId(entity.getApprovedByUser().getId());
        dto.setApprovedByEmployeeId(entity.getApprovedByEmployeeId());
        dto.setApprovedByName(entity.getApprovedByName());
        dto.setRoleCode(entity.getRoleCode());
        dto.setApprovalLevel(entity.getApprovalLevel());

        // Action
        dto.setAction(entity.getAction());
        dto.setRemarks(entity.getRemarks());
        dto.setActionTakenAt(entity.getActionTakenAt());

        // Audit
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }

    // Helper method:
    private String getApproverRoleCode(UserEntity user) {
        // Get role with highest approval level
        return user.getRoles().stream()
                .filter(role -> role.getCanApprove() != null && role.getCanApprove())
                .max((r1, r2) -> {
                    int level1 = r1.getApprovalLevel() != null ? r1.getApprovalLevel() : 0;
                    int level2 = r2.getApprovalLevel() != null ? r2.getApprovalLevel() : 0;
                    return Integer.compare(level1, level2);
                })
                .map(role -> role.getRoleCode())
                .orElse("UNKNOWN");
    }

    // Helper method:
    private int getApproverLevel(UserEntity user) {
        // Get highest approval level from user's roles
        return user.getRoles().stream()
                .map(role -> role.getApprovalLevel() != null ? role.getApprovalLevel() : 0)
                .max(Integer::compare)
                .orElse(0);
    }

    // Helper method:
    private int determineRequiredApprovalLevel(Double loanAmount) {
        if (loanAmount <= 500000) { // ≤ ₹5L
            return 1; // Credit Manager
        } else if (loanAmount <= 2000000) { // ≤ ₹20L
            return 2; // Branch Manager
        } else if (loanAmount <= 10000000) {// ≤ ₹1Cr
            return 3; // Regional Manager
        } else {
            return 4; // Chief Credit Officer
        }
    }

    public List<ApprovalResponseDto> getApprovalHistory(String loanNumber) {
        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        return loanApprovalRepository.findByLoanIdOrderByCreatedAtAsc(loan.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }
}
