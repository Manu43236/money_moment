package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.enums.EmiStatusEnums;
import com.moneymoment.lending.common.enums.PaymentStatusEnums;
import com.moneymoment.lending.common.enums.PaymentTypeEnums;
import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.common.spec.EmiPaymentSpecification;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.dtos.PaymentRequestDto;
import com.moneymoment.lending.dtos.PaymentResponseDto;
import com.moneymoment.lending.entities.EmiPaymentEntity;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.EmiPaymentRepository;
import com.moneymoment.lending.repos.EmiScheduleRepository;
import com.moneymoment.lending.repos.LoanRepo;

@Service
public class EmiPaymentService {

    private final LoanRepo loanRepo;
    private final EmiScheduleRepository emiScheduleRepository;
    private final EmiPaymentRepository emiPaymentRepository;
    private final DpdService dpdService;

    EmiPaymentService(LoanRepo loanRepo, EmiScheduleRepository emiScheduleRepository,
            EmiPaymentRepository emiPaymentRepository, DpdService dpdService) {
        this.loanRepo = loanRepo;
        this.emiScheduleRepository = emiScheduleRepository;
        this.emiPaymentRepository = emiPaymentRepository;
        this.dpdService = dpdService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponseDto> getAll(
            int page, int size,
            LocalDate dateFrom, LocalDate dateTo,
            String paymentStatus, String paymentMode,
            String paymentType, String loanNumber) {

        Specification<EmiPaymentEntity> spec = Specification.allOf(
                EmiPaymentSpecification.paymentDateFrom(dateFrom),
                EmiPaymentSpecification.paymentDateTo(dateTo),
                EmiPaymentSpecification.hasPaymentStatus(paymentStatus),
                EmiPaymentSpecification.hasPaymentMode(paymentMode),
                EmiPaymentSpecification.hasPaymentType(paymentType),
                EmiPaymentSpecification.hasLoanNumber(loanNumber));

        var pageable = PageRequest.of(page, size, Sort.by("paymentDate").descending());
        return PagedResponse.of(emiPaymentRepository.findAll(spec, pageable)
                .map(p -> toDto(p, p.getEmiSchedule(), p.getLoan())));
    }

    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto request) {

        LoanEntity loan = loanRepo.findByLoanNumber(request.getLoanNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", request.getLoanNumber()));

        EmiScheduleEntity emi = emiScheduleRepository.findByLoanIdAndEmiNumber(loan.getId(), request.getEmiNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EMI not found for loan: " + request.getLoanNumber() + ", EMI: " + request.getEmiNumber()));

        if (emi.getStatus().equals(EmiStatusEnums.PAID)) {
            throw new BusinessLogicException("EMI already paid on " + emi.getPaidDate());
        }

        String paymentType;
        Double excessAmount = 0.0;
        Double shortfall = 0.0;

        if (request.getPaymentAmount().equals(emi.getEmiAmount())) {
            paymentType = PaymentTypeEnums.FULL;
        } else if (request.getPaymentAmount() > emi.getEmiAmount()) {
            paymentType = PaymentTypeEnums.EXCESS;
            excessAmount = request.getPaymentAmount() - emi.getEmiAmount();
        } else {
            paymentType = PaymentTypeEnums.PARTIAL;
            shortfall = emi.getEmiAmount() - request.getPaymentAmount();
        }

        EmiPaymentEntity payment = new EmiPaymentEntity();
        payment.setPaymentNumber(NumberGenerator.numberGeneratorWithPrifix(AppConstants.PAYMENT_NUMBER_PREFIX));
        payment.setLoan(loan);
        payment.setCustomer(loan.getCustomer());
        payment.setEmiSchedule(emi);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setPaymentAmount(request.getPaymentAmount());
        payment.setPaymentMode(request.getPaymentMode());
        payment.setTransactionId(request.getTransactionId());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setPaymentType(paymentType);
        payment.setExcessAmount(excessAmount);
        payment.setPaymentStatus(PaymentStatusEnums.SUCCESS);

        payment = emiPaymentRepository.save(payment);

        // Update amount paid
        Double currentPaid = emi.getAmountPaid() != null ? emi.getAmountPaid() : 0.0;
        emi.setAmountPaid(currentPaid + request.getPaymentAmount());

        // Update shortfall
        emi.setShortfallAmount(shortfall);

        // Update status
        if (paymentType.equals("FULL") || paymentType.equals("EXCESS")) {
            emi.setStatus("PAID");
            emi.setPaidDate(request.getPaymentDate());
            emi.setDaysPastDue(0); // Reset DPD
        } else {
            emi.setStatus("PARTIALLY_PAID");
        }

        emi = emiScheduleRepository.save(emi);

        if (emi.getStatus().equals("PAID")) {
            // Increment paid EMIs count
            loan.setNumberOfPaidEmis(loan.getNumberOfPaidEmis() + 1);

            // Update last payment date
            loan.setLastPaymentDate(request.getPaymentDate());

            // Update outstanding amount (reduce by principal)
            loan.setOutstandingAmount(loan.getOutstandingAmount() - emi.getPrincipalAmount());

            // Update next due date (find next unpaid EMI)
            Optional<EmiScheduleEntity> nextEmi = emiScheduleRepository.findByLoanIdAndEmiNumber(
                    loan.getId(),
                    emi.getEmiNumber() + 1);
            if (nextEmi.isPresent()) {
                loan.setNextDueDate(nextEmi.get().getDueDate());
            } else {
                // All EMIs paid - loan fully paid
                loan.setNextDueDate(null);
            }

            loan = loanRepo.save(loan);
        }

        // Update loan status immediately — no need to wait for EOD
        dpdService.updateLoanStatus(loan.getId());

        // Re-fetch loan to return updated status
        loan = loanRepo.findById(loan.getId()).orElse(loan);
        return toDto(payment, emi, loan);

    }

    private PaymentResponseDto toDto(EmiPaymentEntity payment, EmiScheduleEntity emi, LoanEntity loan) {
        PaymentResponseDto dto = new PaymentResponseDto();

        dto.setId(payment.getId());
        dto.setPaymentNumber(payment.getPaymentNumber());

        dto.setLoanId(loan.getId());
        dto.setLoanNumber(loan.getLoanNumber());

        dto.setCustomerId(loan.getCustomer().getId());
        dto.setCustomerNumber(loan.getCustomer().getCustomerNumber());
        dto.setCustomerName(loan.getCustomer().getName());

        dto.setEmiScheduleId(emi.getId());
        dto.setEmiNumber(emi.getEmiNumber());
        dto.setEmiDueDate(emi.getDueDate());
        dto.setEmiAmount(emi.getEmiAmount());

        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentAmount(payment.getPaymentAmount());
        dto.setPaymentMode(payment.getPaymentMode());
        dto.setTransactionId(payment.getTransactionId());
        dto.setReferenceNumber(payment.getReferenceNumber());

        dto.setPaymentType(payment.getPaymentType());
        dto.setExcessAmount(payment.getExcessAmount());

        dto.setPaymentStatus(payment.getPaymentStatus());
        dto.setEmiStatus(emi.getStatus());

        dto.setShortfallAmount(emi.getShortfallAmount());
        dto.setLoanOutstandingAmount(loan.getOutstandingAmount());

        dto.setCreatedAt(payment.getCreatedAt());

        return dto;
    }
}
