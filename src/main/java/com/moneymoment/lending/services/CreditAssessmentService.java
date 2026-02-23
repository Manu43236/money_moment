package com.moneymoment.lending.services;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.enums.LoanStatusEnums;
import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.dtos.CreditAssessmentRequestDto;
import com.moneymoment.lending.dtos.CreditAssessmentResponseDto;
import com.moneymoment.lending.entities.CreditAssessmentEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.master.entities.LoanStatusesEntity;
import com.moneymoment.lending.master.repos.LoanPurposesRepo;
import com.moneymoment.lending.master.repos.LoanStatusesRepo;
import com.moneymoment.lending.repos.CreditAssessmentRepository;
import com.moneymoment.lending.repos.LoanRepo;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditAssessmentService {

    private final LoanStatusesRepo loanStatusesRepo;

    private final LoanRepo loanRepo;

    private final CreditAssessmentRepository creditAssessmentRepository;

    
    CreditAssessmentService(CreditAssessmentRepository creditAssessmentRepository, LoanRepo loanRepo,
            LoanPurposesRepo loanPurposesRepo, LoanStatusesRepo loanStatusesRepo) {
        this.creditAssessmentRepository = creditAssessmentRepository;
        this.loanRepo = loanRepo;
        this.loanStatusesRepo = loanStatusesRepo;
    }

    @Transactional
    public CreditAssessmentResponseDto createCreditAssessment(CreditAssessmentRequestDto creditAssessmentRequestDto) {

        LoanEntity loanEntity = loanRepo.findByLoanNumber(creditAssessmentRequestDto.getLoanNumber())
                .orElseThrow(() -> new BusinessLogicException("Loan number does not exists"));

        if (!loanEntity.getLoanStatus().getCode().equals(LoanStatusEnums.INITIATED)) {
            throw new BusinessLogicException("Loan is not in assessment stage");
        }

        CreditAssessmentEntity creditAssessmentEntity = new CreditAssessmentEntity();

        creditAssessmentEntity.setLoan(loanEntity);
        creditAssessmentEntity.setCustomer(loanEntity.getCustomer());

        creditAssessmentEntity.setCreditScore(loanEntity.getCustomer().getCreditScore());

        creditAssessmentEntity.setCreditScoreSource("CIBIL");

        creditAssessmentEntity.setMonthlyIncome(loanEntity.getCustomer().getMonthlySalary());

        creditAssessmentEntity.setExistingEmiObligations(creditAssessmentRequestDto.getExistingEmiObligations() != null
                ? creditAssessmentRequestDto.getExistingEmiObligations()
                : 0.0);

        creditAssessmentEntity.setProposedEmi(loanEntity.getEmiAmount());

        // dti
        double totalDebt = creditAssessmentRequestDto.getExistingEmiObligations() + loanEntity.getEmiAmount();
        double dtiRatio = (totalDebt / loanEntity.getCustomer().getMonthlySalary()) * 100;
        creditAssessmentEntity.setDtiRatio(Math.round(dtiRatio * 100.0) / 100.0);

        double foir = (totalDebt / loanEntity.getCustomer().getMonthlySalary()) * 100;
        creditAssessmentEntity.setFoir(Math.round(foir * 100.0) / 100.0);

        String riskCategory;
        if (loanEntity.getCustomer().getCreditScore() >= 750) {
            riskCategory = "LOW";
        } else if (loanEntity.getCustomer().getCreditScore() >= 650) {
            riskCategory = "MEDIUM";
        } else {
            riskCategory = "HIGH";
        }
        creditAssessmentEntity.setRiskCategory(riskCategory);

        boolean isEligible = loanEntity.getCustomer().getCreditScore() >= 650 && // Minimum credit score
                dtiRatio <= 50 && // DTI within limit
                loanEntity.getCustomer().getMonthlySalary() >= 15000; // Minimum income (from AppConstants)

        creditAssessmentEntity.setIsEligible(isEligible);

        String recommendation;
        if (!isEligible) {
            recommendation = "REJECT";
        } else if (riskCategory.equals("LOW")) {
            recommendation = "APPROVE";
        } else if (riskCategory.equals("MEDIUM")) {
            recommendation = "MANUAL_REVIEW";
        } else {
            recommendation = "REJECT";
        }
        creditAssessmentEntity.setRecommendation(recommendation);

        StringBuilder remarks = new StringBuilder();

        if (loanEntity.getCustomer().getCreditScore() < 650) {
            remarks.append("Credit score below minimum threshold (650). ");
        }
        if (dtiRatio > 50) {
            remarks.append("DTI ratio " + dtiRatio + "% exceeds limit (50%). ");
        }
        if (loanEntity.getCustomer().getMonthlySalary() < 15000) {
            remarks.append("Monthly income below minimum requirement. ");
        }
        if (isEligible) {
            remarks.append("Customer meets eligibility criteria. ");
        }

        creditAssessmentEntity.setRemarks(remarks.toString());

        creditAssessmentEntity.setAssessedBy(creditAssessmentRequestDto.getAssessedBy()); // Employee ID from request
        creditAssessmentEntity.setAssessedAt(LocalDateTime.now());
        LoanStatusesEntity loanStatus = loanStatusesRepo
                .findByCode(recommendation.equals("APPROVE") ? LoanStatusEnums.UNDER_REVIEW
                        : recommendation.equals("MANUAL_REVIEW") ? LoanStatusEnums.MANUAL_REVIEW
                                : LoanStatusEnums.REJECTED)
                .orElseThrow(() -> new BusinessLogicException("Loan not found"));

        loanEntity.setLoanStatus(loanStatus);

        loanRepo.save(loanEntity);

        creditAssessmentEntity.setLoan(loanEntity);

        creditAssessmentEntity.setAssessmentNumber(
                NumberGenerator.numberGeneratorWithPrifix(AppConstants.CREDIT_ASSESSMENT_NUMBER_PREFIX));

        creditAssessmentEntity = creditAssessmentRepository.save(creditAssessmentEntity);

        return toDto(creditAssessmentEntity);
    }

    private CreditAssessmentResponseDto toDto(CreditAssessmentEntity entity) {
        CreditAssessmentResponseDto dto = new CreditAssessmentResponseDto();

        // Identifiers
        dto.setId(entity.getId());
        dto.setAssessmentNumber(entity.getAssessmentNumber());

        // Loan & Customer
        dto.setLoanId(entity.getLoan().getId());
        dto.setLoanNumber(entity.getLoan().getLoanNumber());
        dto.setCustomerId(entity.getCustomer().getId());
        dto.setCustomerNumber(entity.getCustomer().getCustomerNumber());
        dto.setCustomerName(entity.getCustomer().getName());

        // Credit Score
        dto.setCreditScore(entity.getCreditScore());
        dto.setCreditScoreSource(entity.getCreditScoreSource());

        // Income & Obligations
        dto.setMonthlyIncome(entity.getMonthlyIncome());
        dto.setExistingEmiObligations(entity.getExistingEmiObligations());
        dto.setProposedEmi(entity.getProposedEmi());
        dto.setLoanAmount(entity.getLoan().getLoanAmount());

        // Ratios
        dto.setDtiRatio(entity.getDtiRatio());
        dto.setFoir(entity.getFoir());

        // Assessment Result
        dto.setRiskCategory(entity.getRiskCategory());
        dto.setIsEligible(entity.getIsEligible());
        dto.setRecommendation(entity.getRecommendation());
        dto.setRemarks(entity.getRemarks());

        // Assessed By
        dto.setAssessedBy(entity.getAssessedBy());
        dto.setAssessedAt(entity.getAssessedAt());

        // Audit
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }

    public CreditAssessmentResponseDto getAssessmentById(Long id) {
        CreditAssessmentEntity assessment = creditAssessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CreditAssessment", "id", id));
        return toDto(assessment);
    }

    public CreditAssessmentResponseDto getLatestAssessmentByLoan(String loanNumber) {
        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        CreditAssessmentEntity assessment = creditAssessmentRepository
                .findTopByLoanIdOrderByCreatedAtDesc(loan.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No credit assessment found for loan: " + loanNumber));

        return toDto(assessment);
    }

}
