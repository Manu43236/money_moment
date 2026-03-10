package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.entities.EmiScheduleEntity;
import com.moneymoment.lending.repos.EmiScheduleRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NextDayPrepService {

    private final EmiScheduleRepository emiScheduleRepository;

    public NextDayPrepService(EmiScheduleRepository emiScheduleRepository) {
        this.emiScheduleRepository = emiScheduleRepository;
    }

    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // EMIs due tomorrow (PENDING status, not yet paid)
        List<EmiScheduleEntity> emisDueTomorrow = emiScheduleRepository.findAll().stream()
                .filter(emi -> emi.getDueDate() != null
                        && emi.getDueDate().equals(tomorrow)
                        && (emi.getStatus().equals("PENDING") || emi.getStatus().equals("UPCOMING")))
                .toList();

        double expectedCollection = emisDueTomorrow.stream()
                .mapToDouble(emi -> emi.getEmiAmount() != null ? emi.getEmiAmount() : 0.0)
                .sum();

        metrics.put("businessDate", tomorrow.toString());
        metrics.put("emisDueTomorrow", emisDueTomorrow.size());
        metrics.put("expectedCollectionAmount", Math.round(expectedCollection * 100.0) / 100.0);
        metrics.put("nachMandatesToPresent", emisDueTomorrow.size()); // 1 mandate per EMI
        metrics.put("dateRolloverStatus", "READY");

        log.info("Next Day Prep: {} EMIs due on {} | Expected collection: ₹{}",
                emisDueTomorrow.size(), tomorrow, Math.round(expectedCollection));
        return metrics;
    }
}
