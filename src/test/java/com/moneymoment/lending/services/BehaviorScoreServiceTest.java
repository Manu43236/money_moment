package com.moneymoment.lending.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moneymoment.lending.entities.EmiScheduleEntity;

class BehaviorScoreServiceTest {
    private final LocalDate today = LocalDate.of(2026, 9, 20);

    @Test
    void rewardsOnTimePayments() {
        EmiScheduleEntity emi = emi("PAID", today.minusMonths(1), today.minusMonths(1), 0);
        assertEquals(760.0, BehaviorScoreService.calculate(List.of(emi), today).score());
    }

    @Test
    void penalizesCurrentDelinquencyByDpdBucket() {
        EmiScheduleEntity overdue = emi("OVERDUE", today.minusDays(45), null, 45);
        assertEquals(723.5, BehaviorScoreService.calculate(List.of(overdue), today).score());
    }

    @Test
    void calculationIsDeterministicAndFloored() {
        List<EmiScheduleEntity> severe = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> emi("OVERDUE", today.minusDays(100 + i), null, 100 + i)).toList();
        assertEquals(300.0, BehaviorScoreService.calculate(severe, today).score());
        assertEquals(300.0, BehaviorScoreService.calculate(severe, today).score());
    }

    private EmiScheduleEntity emi(String status, LocalDate dueDate, LocalDate paidDate, int dpd) {
        EmiScheduleEntity emi = new EmiScheduleEntity();
        emi.setStatus(status);
        emi.setDueDate(dueDate);
        emi.setPaidDate(paidDate);
        emi.setDaysPastDue(dpd);
        return emi;
    }
}
