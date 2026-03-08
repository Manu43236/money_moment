package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.dtos.EodResultDto;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.repos.EmiScheduleRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EodService {

    private final DpdService dpdService;
    private final PenaltyService penaltyService;
    private final EmiScheduleRepository emiScheduleRepository;

    public EodService(
            DpdService dpdService,
            PenaltyService penaltyService,
            EmiScheduleRepository emiScheduleRepository) {
        this.dpdService = dpdService;
        this.penaltyService = penaltyService;
        this.emiScheduleRepository = emiScheduleRepository;
    }

    @Transactional
    public EodResultDto processEod() {

        log.info("========== EOD PROCESSING STARTED ==========");
        log.info("EOD Start Time: {}", LocalDateTime.now());

        try {
            // Step 1 + 2: Calculate DPD and update loan statuses
            log.info("Step 1: Calculating DPD and updating loan statuses...");
            EodResultDto result = dpdService.processAllOverdueEmis();
            log.info("Step 1 done — loans: {}, EMIs: {}, overdue EMIs: {}",
                    result.getTotalLoansProcessed(), result.getTotalEmisProcessed(), result.getEmisMarkedOverdue());

            // Step 3: Apply late payment penalties
            log.info("Step 2: Applying late payment penalties...");
            int[] penaltyStats = applyLateFees();
            result.setPenaltiesApplied(penaltyStats[0]);
            result.setTotalPenaltyAmount(penaltyStats[1]);
            log.info("Step 2 done — penalties applied: {}", penaltyStats[0]);

            log.info("========== EOD PROCESSING COMPLETED SUCCESSFULLY ==========");
            log.info("EOD End Time: {}", LocalDateTime.now());

            return result;

        } catch (Exception e) {
            log.error("========== EOD PROCESSING FAILED ==========");
            log.error("Error: {}", e.getMessage(), e);
            throw e;
        }
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