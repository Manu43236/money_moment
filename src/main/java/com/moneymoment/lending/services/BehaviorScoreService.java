package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.dtos.BehaviorScoreHistoryDto;
import com.moneymoment.lending.entities.BehaviorScoreHistoryEntity;
import com.moneymoment.lending.entities.CustomerEntity;
import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.repos.BehaviorScoreHistoryRepository;
import com.moneymoment.lending.repos.CustomerRepository;
import com.moneymoment.lending.repos.EmiScheduleRepository;

@Service
public class BehaviorScoreService {
    static final double INITIAL_SCORE = 758.5;
    static final double MIN_SCORE = 300.0;
    static final double MAX_SCORE = 850.0;

    private final CustomerRepository customerRepo;
    private final EmiScheduleRepository emiRepo;
    private final BehaviorScoreHistoryRepository historyRepo;

    public BehaviorScoreService(CustomerRepository customerRepo, EmiScheduleRepository emiRepo,
            BehaviorScoreHistoryRepository historyRepo) {
        this.customerRepo = customerRepo;
        this.emiRepo = emiRepo;
        this.historyRepo = historyRepo;
    }

    @Transactional
    public double recalculateCustomer(Long customerId, String triggerSource) {
        CustomerEntity customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));
        List<EmiScheduleEntity> emis = emiRepo.findByCustomerIdOrderByDueDateAsc(customerId);
        ScoreResult result = calculate(emis, LocalDate.now());
        double previous = customer.getCreditScore() == null ? INITIAL_SCORE : customer.getCreditScore();

        if (Math.abs(previous - result.score()) >= 0.05) {
            customer.setCreditScore(result.score());
            customerRepo.save(customer);

            BehaviorScoreHistoryEntity history = new BehaviorScoreHistoryEntity();
            history.setCustomer(customer);
            history.setPreviousScore(previous);
            history.setNewScore(result.score());
            history.setScoreChange(round(result.score() - previous));
            history.setTriggerSource(triggerSource);
            history.setReason(result.reason());
            historyRepo.save(history);
        }
        return result.score();
    }

    @Transactional
    public int recalculateAll(String triggerSource) {
        int changed = 0;
        for (Long customerId : emiRepo.findDistinctCustomerIds()) {
            double before = customerRepo.findById(customerId).map(CustomerEntity::getCreditScore).orElse(INITIAL_SCORE);
            double after = recalculateCustomer(customerId, triggerSource);
            if (Math.abs(before - after) >= 0.05) changed++;
        }
        return changed;
    }

    @Transactional(readOnly = true)
    public List<BehaviorScoreHistoryDto> history(Long customerId) {
        return historyRepo.findTop20ByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(this::toDto).toList();
    }

    static ScoreResult calculate(List<EmiScheduleEntity> emis, LocalDate today) {
        double score = INITIAL_SCORE;
        int onTime = 0, late = 0, overdue = 0;
        for (EmiScheduleEntity emi : emis) {
            if ("PAID".equals(emi.getStatus()) && emi.getPaidDate() != null) {
                long lateDays = Math.max(0, ChronoUnit.DAYS.between(emi.getDueDate(), emi.getPaidDate()));
                if (lateDays == 0) { score += 1.5; onTime++; }
                else { score -= penalty(lateDays, true); late++; }
            } else {
                long dpd = emi.getDaysPastDue() != null ? emi.getDaysPastDue()
                        : Math.max(0, ChronoUnit.DAYS.between(emi.getDueDate(), today));
                if (dpd > 0) { score -= penalty(dpd, false); overdue++; }
            }
        }
        score = round(Math.max(MIN_SCORE, Math.min(MAX_SCORE, score)));
        String reason = "Recalculated from EMI performance: " + onTime + " on-time, " + late
                + " paid late, " + overdue + " currently overdue.";
        return new ScoreResult(score, reason);
    }

    private static double penalty(long days, boolean cured) {
        double value = days <= 30 ? 15 : days <= 60 ? 35 : days <= 90 ? 60 : 100;
        return cured ? value * 0.6 : value;
    }

    private BehaviorScoreHistoryDto toDto(BehaviorScoreHistoryEntity entity) {
        BehaviorScoreHistoryDto dto = new BehaviorScoreHistoryDto();
        dto.setId(entity.getId());
        dto.setPreviousScore(entity.getPreviousScore());
        dto.setNewScore(entity.getNewScore());
        dto.setScoreChange(entity.getScoreChange());
        dto.setTriggerSource(entity.getTriggerSource());
        dto.setReason(entity.getReason());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private static double round(double value) { return Math.round(value * 10.0) / 10.0; }

    record ScoreResult(double score, String reason) {}
}
