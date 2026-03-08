package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.dtos.EodLogResponseDto;
import com.moneymoment.lending.dtos.EodResultDto;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.entities.EodLogEntity;
import com.moneymoment.lending.repos.EmiScheduleRepository;
import com.moneymoment.lending.repos.EodLogRepository;
import com.moneymoment.lending.common.response.PagedResponse;

import org.springframework.data.domain.PageRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EodService {

    private final DpdService dpdService;
    private final PenaltyService penaltyService;
    private final EmiScheduleRepository emiScheduleRepository;
    private final EodLogRepository eodLogRepository;

    public EodService(
            DpdService dpdService,
            PenaltyService penaltyService,
            EmiScheduleRepository emiScheduleRepository,
            EodLogRepository eodLogRepository) {
        this.dpdService = dpdService;
        this.penaltyService = penaltyService;
        this.emiScheduleRepository = emiScheduleRepository;
        this.eodLogRepository = eodLogRepository;
    }

    @Transactional
    public EodResultDto processEod() {
        return processEod("MANUAL");
    }

    @Transactional
    public EodResultDto processEod(String triggeredBy) {
        log.info("========== EOD PROCESSING STARTED ({}) ==========", triggeredBy);
        log.info("EOD Start Time: {}", LocalDateTime.now());

        EodLogEntity eodLog = new EodLogEntity();
        eodLog.setRunDate(LocalDateTime.now());
        eodLog.setTriggeredBy(triggeredBy);

        try {
            EodResultDto result = dpdService.processAllOverdueEmis();
            log.info("DPD done — loans: {}, EMIs: {}, overdue: {}",
                    result.getTotalLoansProcessed(), result.getTotalEmisProcessed(), result.getEmisMarkedOverdue());

            int[] penaltyStats = applyLateFees();
            result.setPenaltiesApplied(penaltyStats[0]);
            log.info("Penalties done — applied: {}", penaltyStats[0]);

            // Save success log
            eodLog.setStatus("SUCCESS");
            eodLog.setTotalLoansProcessed(result.getTotalLoansProcessed());
            eodLog.setTotalEmisProcessed(result.getTotalEmisProcessed());
            eodLog.setEmisMarkedOverdue(result.getEmisMarkedOverdue());
            eodLog.setEmisAlreadyPaid(result.getEmisAlreadyPaid());
            eodLog.setLoansMarkedActive(result.getLoansMarkedActive());
            eodLog.setLoansMarkedOverdue(result.getLoansMarkedOverdue());
            eodLog.setLoansMarkedNpa(result.getLoansMarkedNpa());
            eodLog.setLoansStayedDisbursed(result.getLoansStayedDisbursed());
            eodLog.setPenaltiesApplied(result.getPenaltiesApplied());
            eodLogRepository.save(eodLog);

            log.info("========== EOD COMPLETED SUCCESSFULLY ==========");
            return result;

        } catch (Exception e) {
            log.error("========== EOD FAILED ==========");
            log.error("Error: {}", e.getMessage(), e);
            eodLog.setStatus("FAILED");
            eodLog.setErrorMessage(e.getMessage());
            eodLogRepository.save(eodLog);
            throw e;
        }
    }

    public PagedResponse<EodLogResponseDto> getHistory(int page, int size) {
        return PagedResponse.of(
                eodLogRepository.findAllByOrderByRunDateDesc(PageRequest.of(page, size))
                        .map(this::toDto));
    }

    private EodLogResponseDto toDto(EodLogEntity log) {
        EodLogResponseDto dto = new EodLogResponseDto();
        dto.setId(log.getId());
        dto.setRunDate(log.getRunDate());
        dto.setStatus(log.getStatus());
        dto.setTriggeredBy(log.getTriggeredBy());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setTotalLoansProcessed(log.getTotalLoansProcessed());
        dto.setTotalEmisProcessed(log.getTotalEmisProcessed());
        dto.setEmisMarkedOverdue(log.getEmisMarkedOverdue());
        dto.setEmisAlreadyPaid(log.getEmisAlreadyPaid());
        dto.setLoansMarkedActive(log.getLoansMarkedActive());
        dto.setLoansMarkedOverdue(log.getLoansMarkedOverdue());
        dto.setLoansMarkedNpa(log.getLoansMarkedNpa());
        dto.setLoansStayedDisbursed(log.getLoansStayedDisbursed());
        dto.setPenaltiesApplied(log.getPenaltiesApplied());
        return dto;
    }

    private int[] applyLateFees() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<EmiScheduleEntity> newlyOverdueEmis = emiScheduleRepository
                .findByDueDateAndStatus(yesterday, "OVERDUE");

        int count = 0;
        double totalAmount = 0.0;

        for (EmiScheduleEntity emi : newlyOverdueEmis) {
            try {
                if (emi.getDaysPastDue() != null && emi.getDaysPastDue() >= 1) {
                    penaltyService.applyPenalty(emi.getId(), "EMI_OVERDUE_FEE");
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to apply late fee for EMI {}: {}", emi.getId(), e.getMessage());
            }
        }
        return new int[]{count, (int) totalAmount};
    }
}