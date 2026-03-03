package com.moneymoment.lending.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.dtos.LoanTimelineEventDto;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.repos.CreditAssessmentRepository;
import com.moneymoment.lending.repos.DisbursementRepository;
import com.moneymoment.lending.repos.DocumentRepository;
import com.moneymoment.lending.repos.EmiPaymentRepository;
import com.moneymoment.lending.repos.LoanApprovalRepository;
import com.moneymoment.lending.repos.LoanPenaltyRepository;
import com.moneymoment.lending.repos.LoanRepo;

@Service
public class LoanTimelineService {

    private final LoanRepo loanRepo;
    private final DocumentRepository documentRepository;
    private final CreditAssessmentRepository creditAssessmentRepository;
    private final LoanApprovalRepository loanApprovalRepository;
    private final DisbursementRepository disbursementRepository;
    private final EmiPaymentRepository emiPaymentRepository;
    private final LoanPenaltyRepository loanPenaltyRepository;

    public LoanTimelineService(LoanRepo loanRepo,
            DocumentRepository documentRepository,
            CreditAssessmentRepository creditAssessmentRepository,
            LoanApprovalRepository loanApprovalRepository,
            DisbursementRepository disbursementRepository,
            EmiPaymentRepository emiPaymentRepository,
            LoanPenaltyRepository loanPenaltyRepository) {
        this.loanRepo = loanRepo;
        this.documentRepository = documentRepository;
        this.creditAssessmentRepository = creditAssessmentRepository;
        this.loanApprovalRepository = loanApprovalRepository;
        this.disbursementRepository = disbursementRepository;
        this.emiPaymentRepository = emiPaymentRepository;
        this.loanPenaltyRepository = loanPenaltyRepository;
    }

    @Transactional(readOnly = true)
    public List<LoanTimelineEventDto> getTimeline(String loanNumber) {

        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        List<LoanTimelineEventDto> events = new ArrayList<>();

        // 1. Loan Applied
        events.add(LoanTimelineEventDto.builder()
                .eventType("LOAN_APPLIED")
                .title("Loan Application Submitted")
                .description("Loan application of ₹" + String.format("%,.0f", loan.getLoanAmount())
                        + " for " + loan.getTenureMonths() + " months submitted")
                .performedBy(loan.getCreatedBy())
                .timestamp(loan.getCreatedAt())
                .status("SUCCESS")
                .metadata("Loan Number: " + loan.getLoanNumber()
                        + " | Type: " + loan.getLoanType().getName()
                        + " | Purpose: " + loan.getPurpose())
                .build());

        // 2. Documents Uploaded & Verified
        documentRepository.findByLoanId(loan.getId()).forEach(doc -> {
            events.add(LoanTimelineEventDto.builder()
                    .eventType("DOCUMENT_UPLOADED")
                    .title("Document Uploaded")
                    .description(doc.getDocumentType().getName() + " uploaded")
                    .performedBy(doc.getCreatedBy())
                    .timestamp(doc.getCreatedAt())
                    .status("INFO")
                    .metadata("Document: " + doc.getDocumentNumber()
                            + " | File: " + doc.getFileName()
                            + " | Status: " + doc.getUploadStatus())
                    .build());

            if (doc.getVerifiedAt() != null) {
                events.add(LoanTimelineEventDto.builder()
                        .eventType("DOCUMENT_VERIFIED")
                        .title("Document " + doc.getUploadStatus())
                        .description(doc.getDocumentType().getName() + " was " + doc.getUploadStatus().toLowerCase()
                                + (doc.getVerificationNotes() != null ? " — " + doc.getVerificationNotes() : ""))
                        .performedBy(doc.getVerifiedBy())
                        .timestamp(doc.getVerifiedAt())
                        .status(doc.getUploadStatus().equals("VERIFIED") ? "SUCCESS" : "FAILED")
                        .metadata("Document: " + doc.getDocumentNumber())
                        .build());
            }
        });

        // 3. Credit Assessment
        creditAssessmentRepository.findTopByLoanIdOrderByCreatedAtDesc(loan.getId()).ifPresent(assessment -> {
            events.add(LoanTimelineEventDto.builder()
                    .eventType("CREDIT_ASSESSED")
                    .title("Credit Assessment Completed")
                    .description("Credit score: " + assessment.getCreditScore()
                            + " | DTI: " + assessment.getDtiRatio() + "%"
                            + " | Risk: " + assessment.getRiskCategory()
                            + " | Recommendation: " + assessment.getRecommendation())
                    .performedBy(assessment.getAssessedBy())
                    .timestamp(assessment.getAssessedAt())
                    .status(assessment.getRecommendation().equals("APPROVE") ? "SUCCESS"
                            : assessment.getRecommendation().equals("REJECT") ? "FAILED" : "INFO")
                    .metadata("Assessment: " + assessment.getAssessmentNumber()
                            + " | Eligible: " + assessment.getIsEligible())
                    .build());
        });

        // 4. Approval History
        loanApprovalRepository.findByLoanIdOrderByCreatedAtAsc(loan.getId()).forEach(approval -> {
            events.add(LoanTimelineEventDto.builder()
                    .eventType("LOAN_" + approval.getAction())
                    .title("Loan " + approval.getAction() + " by " + approval.getApprovedByName())
                    .description(approval.getRemarks() != null ? approval.getRemarks() : "No remarks")
                    .performedBy(approval.getApprovedByEmployeeId() + " (" + approval.getApprovedByName() + ")")
                    .timestamp(approval.getActionTakenAt())
                    .status(approval.getAction().equals("APPROVE") ? "SUCCESS" : "FAILED")
                    .metadata("Role: " + approval.getRoleCode()
                            + " | Approval Level: " + approval.getApprovalLevel())
                    .build());
        });

        // 5. Disbursement
        disbursementRepository.findByLoanId(loan.getId()).ifPresent(disbursement -> {
            events.add(LoanTimelineEventDto.builder()
                    .eventType("LOAN_DISBURSED")
                    .title("Loan Disbursed")
                    .description("₹" + String.format("%,.0f", disbursement.getNetDisbursement())
                            + " disbursed via " + disbursement.getDisbursementMode()
                            + " to " + disbursement.getBeneficiaryName())
                    .performedBy(disbursement.getDisbursedByEmployeeId())
                    .timestamp(disbursement.getInitiatedAt())
                    .status(disbursement.getStatus().equals("COMPLETED") ? "SUCCESS" : "INFO")
                    .metadata("Disbursement: " + disbursement.getDisbursementNumber()
                            + " | UTR: " + (disbursement.getUtrNumber() != null ? disbursement.getUtrNumber() : "N/A")
                            + " | Net Amount: ₹" + String.format("%,.0f", disbursement.getNetDisbursement()))
                    .build());
        });

        // 6. EMI Payments
        emiPaymentRepository.findByLoanIdOrderByPaymentDateDesc(loan.getId()).forEach(payment -> {
            events.add(LoanTimelineEventDto.builder()
                    .eventType("EMI_PAID")
                    .title("EMI Payment - " + payment.getPaymentType())
                    .description("₹" + String.format("%,.0f", payment.getPaymentAmount())
                            + " paid via " + payment.getPaymentMode()
                            + " | Status: " + payment.getPaymentStatus())
                    .performedBy(payment.getProcessedByUser() != null
                            ? payment.getProcessedByUser().getEmployeeId()
                            : "SYSTEM")
                    .timestamp(payment.getPaymentDate().atStartOfDay())
                    .status(payment.getPaymentStatus().equals("SUCCESS") ? "SUCCESS"
                            : payment.getPaymentStatus().equals("BOUNCED") ? "FAILED" : "INFO")
                    .metadata("Payment: " + payment.getPaymentNumber()
                            + " | TxnId: " + (payment.getTransactionId() != null ? payment.getTransactionId() : "N/A"))
                    .build());
        });

        // 7. Penalties
        loanPenaltyRepository.findByLoanIdOrderByAppliedDateDesc(loan.getId()).forEach(penalty -> {
            events.add(LoanTimelineEventDto.builder()
                    .eventType("PENALTY_APPLIED")
                    .title("Penalty Applied — " + penalty.getPenaltyName())
                    .description("₹" + String.format("%,.0f", penalty.getPenaltyAmount())
                            + " penalty for " + penalty.getDaysOverdue() + " days overdue")
                    .performedBy(penalty.getAppliedBy())
                    .timestamp(penalty.getAppliedDate().atStartOfDay())
                    .status("FAILED")
                    .metadata("Penalty Code: " + penalty.getPenaltyCode()
                            + " | Waived: " + (Boolean.TRUE.equals(penalty.getIsWaived()) ? "Yes" : "No"))
                    .build());

            if (Boolean.TRUE.equals(penalty.getIsWaived()) && penalty.getWaivedAt() != null) {
                events.add(LoanTimelineEventDto.builder()
                        .eventType("PENALTY_WAIVED")
                        .title("Penalty Waived")
                        .description("₹" + String.format("%,.0f", penalty.getWaivedAmount())
                                + " waived — " + penalty.getWaiverReason())
                        .performedBy(penalty.getWaivedByUser() != null
                                ? penalty.getWaivedByUser().getEmployeeId()
                                : "SYSTEM")
                        .timestamp(penalty.getWaivedAt())
                        .status("INFO")
                        .metadata("Penalty Code: " + penalty.getPenaltyCode())
                        .build());
            }
        });

        // 8. Loan Closed
        if (loan.getClosedDate() != null) {
            events.add(LoanTimelineEventDto.builder()
                    .eventType("LOAN_CLOSED")
                    .title("Loan Closed")
                    .description("Loan fully repaid and closed successfully")
                    .performedBy("SYSTEM")
                    .timestamp(loan.getClosedDate())
                    .status("SUCCESS")
                    .metadata("Loan Number: " + loan.getLoanNumber())
                    .build());
        }

        // Sort all events by timestamp ascending
        events.sort(Comparator.comparing(
                LoanTimelineEventDto::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return events;
    }
}
