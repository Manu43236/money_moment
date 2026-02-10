package com.moneymoment.lending.common.utils;

import com.moneymoment.lending.common.constants.AppConstants;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public static String generateCustomerNumber() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        return AppConstants.CUSTOMER_NUMBER_PREFIX + timestamp;
    }

    public static String generateLoanNumber() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        return AppConstants.LOAN_NUMBER_PREFIX + timestamp;
    }

    public static String numberGeneratorWithPrifix(String prefix) {
        return prefix + LocalDateTime.now().format(FORMATTER);
    }
}