package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.enums.LoanStatusEnums;
import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.dtos.EmiScheduleResponseDto;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.EmiScheduleRepository;
import com.moneymoment.lending.repos.LoanRepo;

@Service
public class EMIScheduleGenerationService {

    private final LoanRepo loanRepo;
    private final EmiScheduleRepository emiScheduleRepository;

    public EMIScheduleGenerationService(LoanRepo loanRepo, EmiScheduleRepository emiScheduleRepository) {
        this.loanRepo = loanRepo;
        this.emiScheduleRepository = emiScheduleRepository;
    }

    @Transactional
    public List<EmiScheduleResponseDto> generateSchedule(Long loanId) {

        LoanEntity loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", loanId));

        if (!loan.getLoanStatus().getCode().equals(LoanStatusEnums.DISBURSED)) {
            throw new BusinessLogicException("Can only generate schedule for disbursed loans");
        }

        List<EmiScheduleEntity> existing = emiScheduleRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId());
        if (!existing.isEmpty()) {
            throw new BusinessLogicException("EMI schedule already exists for this loan");
        }

        Double loanAmount = loan.getLoanAmount();
        Double interestRate = loan.getInterestRate(); // Annual rate
        Integer tenureMonths = loan.getTenureMonths();
        Double emiAmount = loan.getEmiAmount();
        LocalDate disbursedDate = loan.getDisbursedDate().toLocalDate();

        // First EMI due 30 days after disbursement
        LocalDate firstDueDate = disbursedDate.plusDays(30);

        Double remainingPrincipal = loanAmount;
        List<EmiScheduleEntity> emiScheduleList = new ArrayList<>();

        for (int i = 1; i <= tenureMonths; i++) {

            // Calculate interest for this month (reducing balance)
            Double monthlyInterestRate = interestRate / 12 / 100; // Convert annual to monthly
            Double interestAmount = remainingPrincipal * monthlyInterestRate;

            // Round to 2 decimals
            interestAmount = Math.round(interestAmount * 100.0) / 100.0;

            // Calculate principal
            Double principalAmount = emiAmount - interestAmount;
            principalAmount = Math.round(principalAmount * 100.0) / 100.0;

            // Handle last EMI adjustment (rounding differences)
            if (i == tenureMonths) {
                principalAmount = remainingPrincipal;
                emiAmount = principalAmount + interestAmount;
            }

            // Calculate due date (monthly)
            LocalDate dueDate = firstDueDate.plusMonths(i - 1);

            // Create EMI record
            EmiScheduleEntity emi = new EmiScheduleEntity();
            emi.setLoan(loan);
            emi.setCustomer(loan.getCustomer());
            emi.setEmiNumber(i);
            emi.setDueDate(dueDate);
            emi.setPrincipalAmount(principalAmount);
            emi.setInterestAmount(interestAmount);
            emi.setEmiAmount(emiAmount);
            emi.setOutstandingPrincipal(remainingPrincipal - principalAmount);
            emi.setStatus("PENDING");
            emi.setAmountPaid(0.0);
            emi.setShortfallAmount(0.0);
            emi.setDaysPastDue(0);

            emiScheduleList.add(emi);

            // Update remaining principal for next iteration
            remainingPrincipal = remainingPrincipal - principalAmount;
        }

        emiScheduleList = emiScheduleRepository.saveAll(emiScheduleList);

        loan.setFirstEmiDueDate(firstDueDate);
        loan.setNextDueDate(firstDueDate);
        loan.setNumberOfPaidEmis(0);
        loan.setNumberOfOverdueEmis(0);
        loanRepo.save(loan);

        return emiScheduleList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private EmiScheduleResponseDto toDto(EmiScheduleEntity entity) {
        EmiScheduleResponseDto dto = new EmiScheduleResponseDto();

        dto.setId(entity.getId());

        dto.setLoanId(entity.getLoan().getId());
        dto.setLoanNumber(entity.getLoan().getLoanNumber());

        dto.setCustomerId(entity.getCustomer().getId());
        dto.setCustomerNumber(entity.getCustomer().getCustomerNumber());
        dto.setCustomerName(entity.getCustomer().getName());

        dto.setEmiNumber(entity.getEmiNumber());
        dto.setDueDate(entity.getDueDate());

        dto.setPrincipalAmount(entity.getPrincipalAmount());
        dto.setInterestAmount(entity.getInterestAmount());
        dto.setEmiAmount(entity.getEmiAmount());

        dto.setOutstandingPrincipal(entity.getOutstandingPrincipal());

        dto.setStatus(entity.getStatus());
        dto.setPaidDate(entity.getPaidDate());
        dto.setAmountPaid(entity.getAmountPaid());
        dto.setShortfallAmount(entity.getShortfallAmount());

        dto.setDaysPastDue(entity.getDaysPastDue());

        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }
}