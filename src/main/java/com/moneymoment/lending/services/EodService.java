package com.moneymoment.lending.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.EodJobStatus;
import com.moneymoment.lending.dtos.EodLogResponseDto;
import com.moneymoment.lending.dtos.EodPhaseResult;
import com.moneymoment.lending.entities.EodLogEntity;
import com.moneymoment.lending.repos.EmiScheduleRepository;
import com.moneymoment.lending.repos.EodLogRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EodService {

    private final DpdService dpdService;
    private final PenaltyService penaltyService;
    private final EmiScheduleRepository emiScheduleRepository;
    private final EodLogRepository eodLogRepository;
    private final PreEodService preEodService;
    private final InterestAccrualService interestAccrualService;
    private final NachProcessingService nachProcessingService;
    private final CollectionsEodService collectionsEodService;
    private final ProvisioningService provisioningService;
    private final RegulatoryService regulatoryService;
    private final EodReportingService eodReportingService;
    private final ArchivalService archivalService;
    private final NextDayPrepService nextDayPrepService;
    private final EodVerificationService eodVerificationService;

    private final AtomicReference<EodJobStatus> currentJob = new AtomicReference<>(buildIdleStatus());

    public EodService(
            DpdService dpdService,
            PenaltyService penaltyService,
            EmiScheduleRepository emiScheduleRepository,
            EodLogRepository eodLogRepository,
            PreEodService preEodService,
            InterestAccrualService interestAccrualService,
            NachProcessingService nachProcessingService,
            CollectionsEodService collectionsEodService,
            ProvisioningService provisioningService,
            RegulatoryService regulatoryService,
            EodReportingService eodReportingService,
            ArchivalService archivalService,
            NextDayPrepService nextDayPrepService,
            EodVerificationService eodVerificationService) {
        this.dpdService = dpdService;
        this.penaltyService = penaltyService;
        this.emiScheduleRepository = emiScheduleRepository;
        this.eodLogRepository = eodLogRepository;
        this.preEodService = preEodService;
        this.interestAccrualService = interestAccrualService;
        this.nachProcessingService = nachProcessingService;
        this.collectionsEodService = collectionsEodService;
        this.provisioningService = provisioningService;
        this.regulatoryService = regulatoryService;
        this.eodReportingService = eodReportingService;
        this.archivalService = archivalService;
        this.nextDayPrepService = nextDayPrepService;
        this.eodVerificationService = eodVerificationService;
    }

    public EodJobStatus getStatus() {
        return currentJob.get();
    }

    public boolean isRunning() {
        return "RUNNING".equals(currentJob.get().getStatus());
    }

    public void processEodPhases(String triggeredBy) {
        if (isRunning()) {
            log.warn("EOD already running — skipping trigger by {}", triggeredBy);
            return;
        }

        String jobId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("========== EOD STARTED | JobId={} | Trigger={} ==========", jobId, triggeredBy);

        EodJobStatus job = new EodJobStatus();
        job.setJobId(jobId);
        job.setStatus("RUNNING");
        job.setTriggeredBy(triggeredBy);
        job.setStartTime(LocalDateTime.now());
        job.setCurrentPhaseNumber(0);
        job.setPhases(buildPhases());
        currentJob.set(job);

        EodLogEntity eodLog = new EodLogEntity();
        eodLog.setJobId(jobId);
        eodLog.setRunDate(LocalDateTime.now());
        eodLog.setTriggeredBy(triggeredBy);
        eodLog.setStatus("RUNNING");
        eodLogRepository.save(eodLog);

        try {
            // Phase 1: Pre-EOD Checks
            Map<String, Object> p1 = runPhase(job, 1);
            if (p1 != null) log.info("Phase 1 done: {}", p1);

            // Phase 2: Loan Processing (DPD + Status + Penalties)
            Map<String, Object> p2 = runPhase(job, 2);

            // Phase 3: Interest Accrual
            Map<String, Object> p3 = runPhase(job, 3);

            // Phase 4: NACH Processing
            Map<String, Object> p4 = runPhase(job, 4);

            // Phase 5: Collections & Alerts
            Map<String, Object> p5 = runPhase(job, 5);

            // Phase 6: Provisioning & Accounting
            Map<String, Object> p6 = runPhase(job, 6);

            // Phase 7: Regulatory Reporting
            Map<String, Object> p7 = runPhase(job, 7);

            // Phase 8: MIS Reports
            Map<String, Object> p8 = runPhase(job, 8);

            // Phase 9: Archival
            Map<String, Object> p9 = runPhase(job, 9);

            // Phase 10: Next Day Prep
            Map<String, Object> p10 = runPhase(job, 10);

            // Phase 11: Verification
            Map<String, Object> p11 = runPhase(job, 11);

            // Finalize job
            job.setStatus("COMPLETED");
            job.setEndTime(LocalDateTime.now());
            job.setDurationSeconds(Duration.between(job.getStartTime(), job.getEndTime()).getSeconds());
            currentJob.set(job);

            // Update DB log
            eodLog.setStatus("SUCCESS");
            eodLog.setCompletedAt(job.getEndTime());
            eodLog.setDurationSeconds(job.getDurationSeconds());
            applyPhaseMetricsToLog(eodLog, job.getPhases());
            eodLogRepository.save(eodLog);

            log.info("========== EOD COMPLETED | JobId={} | Duration={}s ==========", jobId, job.getDurationSeconds());

        } catch (Exception e) {
            log.error("========== EOD FAILED | JobId={} | Error={} ==========", jobId, e.getMessage(), e);

            job.setStatus("FAILED");
            job.setError(e.getMessage());
            job.setEndTime(LocalDateTime.now());
            job.setDurationSeconds(Duration.between(job.getStartTime(), job.getEndTime()).getSeconds());
            currentJob.set(job);

            eodLog.setStatus("FAILED");
            eodLog.setErrorMessage(e.getMessage());
            eodLog.setCompletedAt(job.getEndTime());
            eodLogRepository.save(eodLog);
        }
    }

    private Map<String, Object> runPhase(EodJobStatus job, int phaseNumber) {
        EodPhaseResult phase = job.getPhases().get(phaseNumber - 1);
        phase.setStatus("RUNNING");
        phase.setStartTime(LocalDateTime.now());
        job.setCurrentPhaseNumber(phaseNumber);
        currentJob.set(job);

        log.info("--- Phase {} START: {} ---", phaseNumber, phase.getPhaseName());

        try {
            Map<String, Object> metrics = executePhase(phaseNumber, phase);
            phase.setMetrics(metrics);
            phase.setStatus("COMPLETED");
            log.info("--- Phase {} DONE: {} ({}) ---", phaseNumber, phase.getPhaseName(), phase.getDurationSeconds() + "s");
            return metrics;
        } catch (Exception e) {
            phase.setStatus("FAILED");
            phase.setError(e.getMessage());
            log.error("--- Phase {} FAILED: {} | {} ---", phaseNumber, phase.getPhaseName(), e.getMessage());
            throw new RuntimeException("Phase " + phaseNumber + " (" + phase.getPhaseName() + ") failed: " + e.getMessage(), e);
        } finally {
            phase.setEndTime(LocalDateTime.now());
            phase.setDurationSeconds(Duration.between(phase.getStartTime(), phase.getEndTime()).getSeconds());
            currentJob.set(job);
        }
    }

    private Map<String, Object> executePhase(int phaseNumber, EodPhaseResult phase) {
        switch (phaseNumber) {
            case 1:  return preEodService.run();
            case 2:  return runLoanProcessing();
            case 3:  return interestAccrualService.run();
            case 4:  return nachProcessingService.run();
            case 5:  return collectionsEodService.run();
            case 6:  return provisioningService.run();
            case 7:  return regulatoryService.run();
            case 8:  return eodReportingService.run();
            case 9:  return archivalService.run();
            case 10: return nextDayPrepService.run();
            case 11: return eodVerificationService.run();
            default: throw new IllegalArgumentException("Unknown phase: " + phaseNumber);
        }
    }

    private Map<String, Object> runLoanProcessing() {
        var result = dpdService.processAllOverdueEmis();

        // Apply late fees for overdue EMIs
        int[] penaltyStats = applyLateFees();

        return Map.of(
            "totalLoansProcessed",  result.getTotalLoansProcessed(),
            "totalEmisProcessed",   result.getTotalEmisProcessed(),
            "emisMarkedOverdue",    result.getEmisMarkedOverdue(),
            "emisAlreadyPaid",      result.getEmisAlreadyPaid(),
            "loansMarkedActive",    result.getLoansMarkedActive(),
            "loansMarkedOverdue",   result.getLoansMarkedOverdue(),
            "loansMarkedNpa",       result.getLoansMarkedNpa(),
            "penaltiesApplied",     penaltyStats[0]
        );
    }

    private int[] applyLateFees() {
        var newlyOverdue = emiScheduleRepository.findByDueDateAndStatus(
                java.time.LocalDate.now().minusDays(1), "OVERDUE");
        int count = 0;
        for (var emi : newlyOverdue) {
            try {
                if (emi.getDaysPastDue() != null && emi.getDaysPastDue() >= 1) {
                    penaltyService.applyPenalty(emi.getId(), "EMI_OVERDUE_FEE");
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to apply late fee for EMI {}: {}", emi.getId(), e.getMessage());
            }
        }
        return new int[]{count};
    }

    private void applyPhaseMetricsToLog(EodLogEntity log, List<EodPhaseResult> phases) {
        for (EodPhaseResult phase : phases) {
            Map<String, Object> m = phase.getMetrics();
            if (m == null) continue;
            switch (phase.getPhaseNumber()) {
                case 2:
                    log.setTotalLoansProcessed(toInt(m.get("totalLoansProcessed")));
                    log.setTotalEmisProcessed(toInt(m.get("totalEmisProcessed")));
                    log.setEmisMarkedOverdue(toInt(m.get("emisMarkedOverdue")));
                    log.setEmisAlreadyPaid(toInt(m.get("emisAlreadyPaid")));
                    log.setLoansMarkedActive(toInt(m.get("loansMarkedActive")));
                    log.setLoansMarkedOverdue(toInt(m.get("loansMarkedOverdue")));
                    log.setLoansMarkedNpa(toInt(m.get("loansMarkedNpa")));
                    log.setPenaltiesApplied(toInt(m.get("penaltiesApplied")));
                    break;
                case 3:
                    log.setInterestAccrued(toDouble(m.get("totalDailyInterestAccrued")));
                    break;
                case 4:
                    log.setNachProcessed(toInt(m.get("mandatesPresented")));
                    log.setNachBounced(toInt(m.get("bounceCount")));
                    break;
                case 5:
                    log.setCustomersAlerted(toInt(m.get("customersAlerted")));
                    break;
                case 6:
                    log.setTotalProvisionAmount(toDouble(m.get("totalProvisionAmount")));
                    break;
                case 10:
                    log.setEmisDueTomorrow(toInt(m.get("emisDueTomorrow")));
                    break;
                case 11:
                    Object rec = m.get("reconciliationPassed");
                    if (rec instanceof Boolean) log.setReconciliationPassed((Boolean) rec);
                    break;
            }
        }
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Long) return ((Long) val).intValue();
        if (val instanceof Number) return ((Number) val).intValue();
        return null;
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Double) return (Double) val;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return null;
    }

    private List<EodPhaseResult> buildPhases() {
        List<EodPhaseResult> phases = new ArrayList<>();
        phases.add(new EodPhaseResult(1,  "Pre-EOD Checks",        "Health checks, transaction cutoff, backup verification"));
        phases.add(new EodPhaseResult(2,  "Loan Processing",        "DPD calculation, loan status update, penalty application"));
        phases.add(new EodPhaseResult(3,  "Interest Accrual",       "Daily interest accrual on outstanding principal"));
        phases.add(new EodPhaseResult(4,  "NACH Processing",        "Mandate presentation, payment collection, bounce handling"));
        phases.add(new EodPhaseResult(5,  "Collections & Alerts",   "Generate collection lists, send SMS/email alerts by DPD bucket"));
        phases.add(new EodPhaseResult(6,  "Provisioning & GL",      "RBI NPA provisioning calculation, General Ledger entries"));
        phases.add(new EodPhaseResult(7,  "Regulatory Reporting",   "RBI NPA report generation, CIBIL bureau upload"));
        phases.add(new EodPhaseResult(8,  "MIS Reports",            "Portfolio MIS, branch performance, operational reports"));
        phases.add(new EodPhaseResult(9,  "Archival",               "Archive closed loans, purge temporary data"));
        phases.add(new EodPhaseResult(10, "Next Day Preparation",   "Tomorrow's EMI schedule, NACH mandate list, date rollover"));
        phases.add(new EodPhaseResult(11, "Verification",           "Reconciliation, data integrity checks, EOD sign-off"));
        return phases;
    }

    private static EodJobStatus buildIdleStatus() {
        EodJobStatus status = new EodJobStatus();
        status.setStatus("IDLE");
        return status;
    }

    public PagedResponse<EodLogResponseDto> getHistory(int page, int size) {
        return PagedResponse.of(
                eodLogRepository.findAllByOrderByRunDateDesc(PageRequest.of(page, size))
                        .map(this::toDto));
    }

    private EodLogResponseDto toDto(EodLogEntity log) {
        EodLogResponseDto dto = new EodLogResponseDto();
        dto.setId(log.getId());
        dto.setJobId(log.getJobId());
        dto.setRunDate(log.getRunDate());
        dto.setCompletedAt(log.getCompletedAt());
        dto.setDurationSeconds(log.getDurationSeconds());
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
        dto.setPenaltiesApplied(log.getPenaltiesApplied());
        dto.setInterestAccrued(log.getInterestAccrued());
        dto.setNachProcessed(log.getNachProcessed());
        dto.setNachBounced(log.getNachBounced());
        dto.setCustomersAlerted(log.getCustomersAlerted());
        dto.setTotalProvisionAmount(log.getTotalProvisionAmount());
        dto.setEmisDueTomorrow(log.getEmisDueTomorrow());
        dto.setReconciliationPassed(log.getReconciliationPassed());
        return dto;
    }
}
