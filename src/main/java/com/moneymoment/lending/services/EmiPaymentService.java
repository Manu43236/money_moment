package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.enums.EmiStatusEnums;
import com.moneymoment.lending.common.enums.LoanStatusEnums;
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
import com.moneymoment.lending.entities.LoanPenaltyEntity;
import com.moneymoment.lending.repos.EmiPaymentRepository;
import com.moneymoment.lending.repos.EmiScheduleRepository;
import com.moneymoment.lending.repos.LoanPenaltyRepository;
import com.moneymoment.lending.repos.LoanRepo;

@Service
public class EmiPaymentService {

    private final LoanRepo loanRepo;
    private final EmiScheduleRepository emiScheduleRepository;
    private final EmiPaymentRepository emiPaymentRepository;
    private final LoanPenaltyRepository loanPenaltyRepository;
    private final DpdService dpdService;

    EmiPaymentService(LoanRepo loanRepo, EmiScheduleRepository emiScheduleRepository,
            EmiPaymentRepository emiPaymentRepository, LoanPenaltyRepository loanPenaltyRepository,
            DpdService dpdService) {
        this.loanRepo = loanRepo;
        this.emiScheduleRepository = emiScheduleRepository;
        this.emiPaymentRepository = emiPaymentRepository;
        this.loanPenaltyRepository = loanPenaltyRepository;
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

        if (EmiStatusEnums.PAID.equals(emi.getStatus())) {
            throw new BusinessLogicException("EMI #" + request.getEmiNumber() + " already paid on " + emi.getPaidDate());
        }

        // Fix 1: Oldest-overdue-first — must clear oldest overdue/partially-paid EMI before skipping ahead
        Optional<EmiScheduleEntity> oldestPending = emiScheduleRepository
                .findFirstByLoanIdAndStatusInOrderByEmiNumberAsc(loan.getId(),
                        List.of(EmiStatusEnums.OVERDUE, EmiStatusEnums.PARTIALLY_PAID));
        if (oldestPending.isPresent() && oldestPending.get().getEmiNumber() < request.getEmiNumber()) {
            EmiScheduleEntity oldest = oldestPending.get();
            throw new BusinessLogicException(
                    "Cannot pay EMI #" + request.getEmiNumber() + ". Please clear EMI #"
                            + oldest.getEmiNumber() + " (due " + oldest.getDueDate() + ") first.");
        }

        // Fix 2: Penalty-first allocation — settle all unpaid penalties before applying to EMI
        List<LoanPenaltyEntity> unpaidPenalties = loanPenaltyRepository
                .findByLoanIdAndIsPaid(loan.getId(), false)
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsWaived()))
                .sorted(Comparator.comparing(LoanPenaltyEntity::getAppliedDate))
                .collect(Collectors.toList());

        double remainingPayment = request.getPaymentAmount();
        if (!unpaidPenalties.isEmpty()) {
            for (LoanPenaltyEntity penalty : unpaidPenalties) {
                if (remainingPayment <= 0) break;
                double alreadyPaid = penalty.getPaidAmount() != null ? penalty.getPaidAmount() : 0.0;
                double penaltyDue = penalty.getPenaltyAmount() - alreadyPaid;
                if (penaltyDue <= 0) continue;
                double penaltyPayment = Math.min(remainingPayment, penaltyDue);
                penalty.setPaidAmount(alreadyPaid + penaltyPayment);
                if (penalty.getPaidAmount() >= penalty.getPenaltyAmount()) {
                    penalty.setIsPaid(true);
                    penalty.setPaidDate(request.getPaymentDate());
                }
                remainingPayment -= penaltyPayment;
            }
            loanPenaltyRepository.saveAll(unpaidPenalties);
        }

        // Fix 5: For partially-paid EMIs, base payment type on the remaining due amount, not the full EMI amount
        double alreadyPaidOnEmi = emi.getAmountPaid() != null ? emi.getAmountPaid() : 0.0;
        double remainingEmiDue = emi.getEmiAmount() - alreadyPaidOnEmi;

        String paymentType;
        double excessAmount = 0.0;
        double shortfall = 0.0;

        if (Math.abs(remainingPayment - remainingEmiDue) < 0.01) {
            paymentType = PaymentTypeEnums.FULL;
        } else if (remainingPayment > remainingEmiDue) {
            paymentType = PaymentTypeEnums.EXCESS;
            excessAmount = remainingPayment - remainingEmiDue;
        } else {
            paymentType = PaymentTypeEnums.PARTIAL;
            shortfall = remainingEmiDue - remainingPayment;
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

        // Update EMI — only credit the amount that went to the EMI (after penalty deduction)
        emi.setAmountPaid(alreadyPaidOnEmi + (remainingPayment - excessAmount));
        emi.setShortfallAmount(shortfall);

        if (PaymentTypeEnums.FULL.equals(paymentType) || PaymentTypeEnums.EXCESS.equals(paymentType)) {
            emi.setStatus(EmiStatusEnums.PAID);
            emi.setPaidDate(request.getPaymentDate());
            emi.setDaysPastDue(0);
        } else {
            emi.setStatus(EmiStatusEnums.PARTIALLY_PAID);
        }

        emi = emiScheduleRepository.save(emi);

        if (EmiStatusEnums.PAID.equals(emi.getStatus())) {
            loan.setNumberOfPaidEmis(loan.getNumberOfPaidEmis() + 1);
            loan.setLastPaymentDate(request.getPaymentDate());
            loan.setOutstandingAmount(loan.getOutstandingAmount() - emi.getPrincipalAmount());

            // Fix 4: Track consecutive payments made while in NPA — need 3 to upgrade to ACTIVE
            if (LoanStatusEnums.NPA.equals(loan.getLoanStatus().getCode())) {
                int current = loan.getNpaRecoveryPaymentCount() != null ? loan.getNpaRecoveryPaymentCount() : 0;
                loan.setNpaRecoveryPaymentCount(current + 1);
            }

            // Fix 3: Set nextDueDate to the earliest unpaid EMI (not blindly emiNumber + 1)
            Optional<EmiScheduleEntity> nextUnpaid = emiScheduleRepository
                    .findFirstByLoanIdAndStatusInOrderByEmiNumberAsc(loan.getId(),
                            List.of(EmiStatusEnums.PENDING, EmiStatusEnums.OVERDUE, EmiStatusEnums.PARTIALLY_PAID));
            loan.setNextDueDate(nextUnpaid.map(EmiScheduleEntity::getDueDate).orElse(null));

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
