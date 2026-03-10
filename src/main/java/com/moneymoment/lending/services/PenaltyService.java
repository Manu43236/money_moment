package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.LoanPenaltyResponseDto;

import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.entities.LoanPenaltyEntity;
import com.moneymoment.lending.entities.PenaltyConfigEntity;
import com.moneymoment.lending.repos.EmiScheduleRepository;
import com.moneymoment.lending.repos.LoanPenaltyRepository;
import com.moneymoment.lending.repos.LoanRepo;
import com.moneymoment.lending.repos.PenaltyConfigRepository;

@Service
public class PenaltyService {

    private final LoanRepo loanRepo;
    private final EmiScheduleRepository emiScheduleRepository;
    private final PenaltyConfigRepository penaltyConfigRepository;
    private final LoanPenaltyRepository loanPenaltyRepository;

    public PenaltyService(
            LoanRepo loanRepo,
            EmiScheduleRepository emiScheduleRepository,
            PenaltyConfigRepository penaltyConfigRepository,
            LoanPenaltyRepository loanPenaltyRepository) {
        this.loanRepo = loanRepo;
        this.emiScheduleRepository = emiScheduleRepository;
        this.penaltyConfigRepository = penaltyConfigRepository;
        this.loanPenaltyRepository = loanPenaltyRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoanPenaltyEntity applyPenalty(Long emiScheduleId, String penaltyCode) {

        // Step 1: Fetch EMI Schedule
        EmiScheduleEntity emi = emiScheduleRepository.findById(emiScheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("EMI", "id", emiScheduleId));

        // Step 2: Fetch Penalty Config
        PenaltyConfigEntity config = penaltyConfigRepository.findByPenaltyCode(penaltyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty config", "code", penaltyCode));

        // Step 3: Check if penalty already applied for this EMI
        List<LoanPenaltyEntity> existing = loanPenaltyRepository.findByEmiScheduleId(emi.getId());
        boolean penaltyExists = existing.stream()
                .anyMatch(p -> p.getPenaltyCode().equals(penaltyCode) && !p.getIsWaived());

        if (penaltyExists) {
            throw new BusinessLogicException(
                    "Penalty " + penaltyCode + " already applied for this EMI");
        }

        // Step 4: Calculate penalty amount
        Double penaltyAmount;
        Double baseAmount = null;

        if (config.getChargeType().equals("FLAT")) {
            penaltyAmount = config.getChargeValue();
        } else {
            // PERCENTAGE - calculate on EMI amount or overdue amount
            baseAmount = emi.getEmiAmount();
            penaltyAmount = (baseAmount * config.getChargeValue()) / 100;

            // Apply max cap if exists
            if (config.getMaxPenaltyAmount() != null && penaltyAmount > config.getMaxPenaltyAmount()) {
                penaltyAmount = config.getMaxPenaltyAmount();
            }
        }

        // Round to 2 decimals
        penaltyAmount = Math.round(penaltyAmount * 100.0) / 100.0;

        // Step 5: Create penalty record
        LoanPenaltyEntity penalty = new LoanPenaltyEntity();
        penalty.setLoan(emi.getLoan());
        penalty.setCustomer(emi.getCustomer());
        penalty.setEmiSchedule(emi);
        penalty.setPenaltyConfig(config);
        penalty.setPenaltyCode(config.getPenaltyCode());
        penalty.setPenaltyName(config.getPenaltyName());
        penalty.setPenaltyAmount(penaltyAmount);
        penalty.setBaseAmount(baseAmount);
        penalty.setDaysOverdue(emi.getDaysPastDue());
        penalty.setIsWaived(false);
        penalty.setWaivedAmount(0.0);
        penalty.setIsPaid(false);
        penalty.setPaidAmount(0.0);
        penalty.setAppliedDate(LocalDate.now());
        penalty.setAppliedBy("SYSTEM");
        penalty.setRemarks("Auto-applied by system");

        penalty = loanPenaltyRepository.save(penalty);

        // Step 6: Update loan total penalty amount
        LoanEntity loan = emi.getLoan();
        Double currentPenalty = loan.getTotalPenaltyAmount() != null ? loan.getTotalPenaltyAmount() : 0.0;
        loan.setTotalPenaltyAmount(currentPenalty + penaltyAmount);
        loanRepo.save(loan);

        return penalty;
    }

    public List<PenaltyConfigEntity> getAllPenaltyConfigs() {
        return penaltyConfigRepository.findAll();
    }

    @Transactional
    public PagedResponse<LoanPenaltyResponseDto> getAllPenalties(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("appliedDate").descending());
        return PagedResponse.of(loanPenaltyRepository.findAll(pageable).map(this::toDto));
    }

    @Transactional
    public PagedResponse<LoanPenaltyResponseDto> getPenaltiesByLoan(String loanNumber, int page, int size) {
        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        var pageable = PageRequest.of(page, size, Sort.by("appliedDate").descending());
        return PagedResponse.of(loanPenaltyRepository.findByLoanId(loan.getId(), pageable).map(this::toDto));
    }

    @Transactional
    public List<LoanPenaltyResponseDto> getPenaltiesByEmi(Long emiScheduleId) {
        return loanPenaltyRepository.findByEmiScheduleId(emiScheduleId).stream().map(this::toDto).toList();
    }

    private LoanPenaltyResponseDto toDto(LoanPenaltyEntity p) {
        LoanPenaltyResponseDto dto = new LoanPenaltyResponseDto();
        dto.setId(p.getId());
        if (p.getLoan() != null) {
            dto.setLoanId(p.getLoan().getId());
            dto.setLoanNumber(p.getLoan().getLoanNumber());
        }
        if (p.getCustomer() != null) {
            dto.setCustomerId(p.getCustomer().getId());
            dto.setCustomerName(p.getCustomer().getName());
        }
        if (p.getEmiSchedule() != null) {
            dto.setEmiScheduleId(p.getEmiSchedule().getId());
            dto.setEmiNumber(p.getEmiSchedule().getEmiNumber());
            dto.setEmiDueDate(p.getEmiSchedule().getDueDate());
        }
        dto.setPenaltyCode(p.getPenaltyCode());
        dto.setPenaltyName(p.getPenaltyName());
        dto.setPenaltyAmount(p.getPenaltyAmount());
        dto.setBaseAmount(p.getBaseAmount());
        dto.setDaysOverdue(p.getDaysOverdue());
        dto.setIsWaived(p.getIsWaived());
        dto.setWaivedAmount(p.getWaivedAmount());
        dto.setWaivedAt(p.getWaivedAt());
        dto.setWaiverReason(p.getWaiverReason());
        dto.setIsPaid(p.getIsPaid());
        dto.setPaidAmount(p.getPaidAmount());
        dto.setPaidDate(p.getPaidDate());
        dto.setAppliedDate(p.getAppliedDate());
        dto.setAppliedBy(p.getAppliedBy());
        dto.setRemarks(p.getRemarks());
        return dto;
    }

    @Transactional
    public LoanPenaltyEntity waivePenalty(Long penaltyId, Long waivedByUserId, String reason) {
        LoanPenaltyEntity penalty = loanPenaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty", "id", penaltyId));

        if (penalty.getIsWaived()) {
            throw new BusinessLogicException("Penalty already waived");
        }

        // Waive the penalty
        penalty.setIsWaived(true);
        penalty.setWaivedAmount(penalty.getPenaltyAmount());
        // penalty.setWaivedByUser(userEntity); // Set if you fetch user
        penalty.setWaivedAt(java.time.LocalDateTime.now());
        penalty.setWaiverReason(reason);

        penalty = loanPenaltyRepository.save(penalty);

        // Update loan total penalty (reduce)
        LoanEntity loan = penalty.getLoan();
        loan.setTotalPenaltyAmount(loan.getTotalPenaltyAmount() - penalty.getPenaltyAmount());
        loanRepo.save(loan);

        return penalty;
    }
}