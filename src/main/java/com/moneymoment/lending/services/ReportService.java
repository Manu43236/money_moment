package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.dtos.ReportCollectionDto;
import com.moneymoment.lending.dtos.ReportDisbursementDto;
import com.moneymoment.lending.dtos.ReportDpdBucketDto;
import com.moneymoment.lending.dtos.ReportLoanBookDto;
import com.moneymoment.lending.dtos.ReportNpaLoanDto;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.EmiScheduleRepository;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReportService {

    private final LoanRepo loanRepo;
    private final EmiScheduleRepository emiScheduleRepository;

    public ReportService(LoanRepo loanRepo,
                         EmiScheduleRepository emiScheduleRepository) {
        this.loanRepo = loanRepo;
        this.emiScheduleRepository = emiScheduleRepository;
    }

    // ─── 1. Loan Book Summary ───────────────────────────────────────────────

    public List<ReportLoanBookDto> getLoanBookSummary() {
        List<LoanEntity> allLoans = loanRepo.findAll();

        return allLoans.stream()
                .collect(Collectors.groupingBy(l -> l.getLoanStatus().getCode()))
                .entrySet().stream()
                .map(entry -> {
                    String status = entry.getKey();
                    List<LoanEntity> loans = entry.getValue();
                    long count = loans.size();
                    double totalLoan = loans.stream().mapToDouble(l -> l.getLoanAmount() != null ? l.getLoanAmount() : 0).sum();
                    double totalOutstanding = loans.stream().mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0).sum();
                    return new ReportLoanBookDto(status, count, totalLoan, totalOutstanding);
                })
                .sorted((a, b) -> a.getStatusCode().compareTo(b.getStatusCode()))
                .collect(Collectors.toList());
    }

    // ─── 2. Collection Report ───────────────────────────────────────────────

    public ReportCollectionDto getCollectionReport(LocalDate from, LocalDate to) {
        // EMIs due in range
        List<EmiScheduleEntity> emisDue = emiScheduleRepository.findByDueDateBetween(from, to);

        long totalDue = emisDue.size();
        long collected = emisDue.stream().filter(e -> "PAID".equals(e.getStatus())).count();
        long pending = emisDue.stream().filter(e -> "PENDING".equals(e.getStatus())).count();
        long overdue = emisDue.stream().filter(e -> "OVERDUE".equals(e.getStatus())).count();

        double amountDue = emisDue.stream().mapToDouble(e -> e.getEmiAmount() != null ? e.getEmiAmount() : 0).sum();
        double amountCollected = emisDue.stream()
                .filter(e -> "PAID".equals(e.getStatus()))
                .mapToDouble(e -> e.getAmountPaid() != null ? e.getAmountPaid() : 0)
                .sum();

        double efficiency = totalDue > 0 ? (amountCollected / amountDue) * 100 : 0;

        return new ReportCollectionDto(totalDue, collected, pending, overdue,
                amountDue, amountCollected, Math.round(efficiency * 100.0) / 100.0);
    }

    // ─── 3. DPD & NPA Aging Bucket Report ──────────────────────────────────

    public List<ReportDpdBucketDto> getDpdAgingReport() {
        List<LoanEntity> activeLoans = loanRepo.findAll().stream()
                .filter(l -> {
                    String code = l.getLoanStatus().getCode();
                    return "ACTIVE".equals(code) || "OVERDUE".equals(code) || "NPA".equals(code)
                            || "DISBURSED".equals(code) || "CURRENT".equals(code);
                })
                .collect(Collectors.toList());

        List<LoanEntity> current = activeLoans.stream().filter(l -> dpd(l) == 0).collect(Collectors.toList());
        List<LoanEntity> sma0 = activeLoans.stream().filter(l -> dpd(l) >= 1 && dpd(l) <= 30).collect(Collectors.toList());
        List<LoanEntity> sma1 = activeLoans.stream().filter(l -> dpd(l) >= 31 && dpd(l) <= 60).collect(Collectors.toList());
        List<LoanEntity> sma2 = activeLoans.stream().filter(l -> dpd(l) >= 61 && dpd(l) <= 90).collect(Collectors.toList());
        List<LoanEntity> npa = activeLoans.stream().filter(l -> dpd(l) > 90).collect(Collectors.toList());

        List<ReportDpdBucketDto> result = new ArrayList<>();
        result.add(toBucketDto("CURRENT", "0 days", current));
        result.add(toBucketDto("SMA-0", "1–30 days", sma0));
        result.add(toBucketDto("SMA-1", "31–60 days", sma1));
        result.add(toBucketDto("SMA-2", "61–90 days", sma2));
        result.add(toBucketDto("NPA", "90+ days", npa));
        return result;
    }

    private int dpd(LoanEntity l) {
        return l.getCurrentDpd() != null ? l.getCurrentDpd() : 0;
    }

    private ReportDpdBucketDto toBucketDto(String bucket, String range, List<LoanEntity> loans) {
        long count = loans.size();
        double outstanding = loans.stream().mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0).sum();
        double overdue = loans.stream().mapToDouble(l -> l.getTotalOverdueAmount() != null ? l.getTotalOverdueAmount() : 0).sum();
        return new ReportDpdBucketDto(bucket, range, count, outstanding, overdue);
    }

    // ─── 4. NPA Loan List ───────────────────────────────────────────────────

    public List<ReportNpaLoanDto> getNpaLoans() {
        return loanRepo.findAll().stream()
                .filter(l -> "NPA".equals(l.getLoanStatus().getCode()))
                .map(l -> new ReportNpaLoanDto(
                        l.getLoanNumber(),
                        l.getCustomer().getName(),
                        l.getCustomer().getCustomerNumber(),
                        l.getLoanAmount(),
                        l.getOutstandingAmount(),
                        l.getTotalOverdueAmount() != null ? l.getTotalOverdueAmount() : 0.0,
                        l.getCurrentDpd() != null ? l.getCurrentDpd() : 0,
                        l.getHighestDpd() != null ? l.getHighestDpd() : 0,
                        l.getNumberOfOverdueEmis() != null ? l.getNumberOfOverdueEmis() : 0))
                .sorted((a, b) -> Integer.compare(b.getCurrentDpd(), a.getCurrentDpd()))
                .collect(Collectors.toList());
    }

    // ─── 5. Disbursement Report ─────────────────────────────────────────────

    public List<ReportDisbursementDto> getDisbursementReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        return loanRepo.findAll().stream()
                .filter(l -> l.getDisbursedDate() != null
                        && !l.getDisbursedDate().isBefore(fromDt)
                        && !l.getDisbursedDate().isAfter(toDt))
                .map(l -> new ReportDisbursementDto(
                        l.getLoanNumber(),
                        l.getCustomer().getName(),
                        l.getCustomer().getCustomerNumber(),
                        l.getLoanType().getName(),
                        l.getLoanAmount(),
                        l.getProcessingFee(),
                        l.getInterestRate(),
                        l.getTenureMonths(),
                        l.getEmiAmount(),
                        l.getDisbursedDate()))
                .sorted((a, b) -> b.getDisbursedDate().compareTo(a.getDisbursedDate()))
                .collect(Collectors.toList());
    }
}
