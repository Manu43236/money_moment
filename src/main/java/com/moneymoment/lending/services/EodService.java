package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void processEod() {

        log.info("========== EOD PROCESSING STARTED ==========");
        log.info("EOD Start Time: {}", LocalDateTime.now());

        try {
            // Step 1: Calculate DPD for all loans
            log.info("Step 1: Calculating DPD for all loans...");
            dpdService.processAllOverdueEmis();
            log.info("Step 1: DPD calculation completed");

            // Step 2: Apply late payment penalties
            log.info("Step 2: Applying late payment penalties...");
            applyLateFees();
            log.info("Step 2: Late payment penalties applied");

            // Step 3: Update loan statuses
            log.info("Step 3: Loan statuses updated via DPD service");

            log.info("========== EOD PROCESSING COMPLETED SUCCESSFULLY ==========");
            log.info("EOD End Time: {}", LocalDateTime.now());

        } catch (Exception e) {
            log.error("========== EOD PROCESSING FAILED ==========");
            log.error("Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void applyLateFees() {
        // Get all OVERDUE EMIs that became overdue today (DPD = 1)
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<EmiScheduleEntity> newlyOverdueEmis = emiScheduleRepository
                .findByDueDateAndStatus(yesterday, "OVERDUE");

        log.info("Found {} newly overdue EMIs", newlyOverdueEmis.size());

        for (EmiScheduleEntity emi : newlyOverdueEmis) {
            try {
                // Check if EMI became overdue exactly 1 day ago
                if (emi.getDaysPastDue() != null && emi.getDaysPastDue() >= 1) {
                    penaltyService.applyPenalty(emi.getId(), "EMI_OVERDUE_FEE");
                    log.info("Late fee applied for EMI {} of Loan {}",
                            emi.getEmiNumber(), emi.getLoan().getLoanNumber());
                }
            } catch (Exception e) {
                log.error("Failed to apply late fee for EMI {}: {}", emi.getId(), e.getMessage());
                // Continue processing other EMIs
            }
        }
    }
}