package com.moneymoment.lending.common.utils;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    
    public static int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
    
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    public static LocalDate addMonths(LocalDate date, int months) {
        return date.plusMonths(months);
    }
}