package com.moneymoment.lending.services;

import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.common.spec.EmiScheduleSpecification;
import com.moneymoment.lending.dtos.EmiScheduleResponseDto;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.repos.EmiScheduleRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class EmiScheduleService {

    private final EmiScheduleRepository emiScheduleRepository;

    public EmiScheduleService(EmiScheduleRepository emiScheduleRepository) {
        this.emiScheduleRepository = emiScheduleRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmiScheduleResponseDto> fetchAll(
            int page, int size,
            String status,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            Integer dpdMin,
            Integer dpdMax,
            String loanNumber) {

        Specification<EmiScheduleEntity> spec = Specification.allOf(
                EmiScheduleSpecification.hasStatus(status),
                EmiScheduleSpecification.dueDateFrom(dueDateFrom),
                EmiScheduleSpecification.dueDateTo(dueDateTo),
                EmiScheduleSpecification.dpdFrom(dpdMin),
                EmiScheduleSpecification.dpdTo(dpdMax),
                EmiScheduleSpecification.hasLoanNumber(loanNumber));

        var pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());
        return PagedResponse.of(emiScheduleRepository.findAll(spec, pageable).map(this::toDto));
    }

    private EmiScheduleResponseDto toDto(EmiScheduleEntity e) {
        EmiScheduleResponseDto dto = new EmiScheduleResponseDto();
        dto.setId(e.getId());
        dto.setLoanId(e.getLoan().getId());
        dto.setLoanNumber(e.getLoan().getLoanNumber());
        dto.setCustomerId(e.getCustomer().getId());
        dto.setCustomerNumber(e.getCustomer().getCustomerNumber());
        dto.setCustomerName(e.getCustomer().getName());
        dto.setEmiNumber(e.getEmiNumber());
        dto.setDueDate(e.getDueDate());
        dto.setPrincipalAmount(e.getPrincipalAmount());
        dto.setInterestAmount(e.getInterestAmount());
        dto.setEmiAmount(e.getEmiAmount());
        dto.setOutstandingPrincipal(e.getOutstandingPrincipal());
        dto.setStatus(e.getStatus());
        dto.setPaidDate(e.getPaidDate());
        dto.setAmountPaid(e.getAmountPaid());
        dto.setShortfallAmount(e.getShortfallAmount());
        dto.setDaysPastDue(e.getDaysPastDue());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
