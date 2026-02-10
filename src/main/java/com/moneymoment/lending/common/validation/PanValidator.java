package com.moneymoment.lending.common.validation;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.exception.ValidationException;

public class PanValidator {

    public static void validate(String pan) {
        if (pan == null || pan.trim().isEmpty()) {
            throw new ValidationException("PAN number is required");
        }

        if (!pan.matches(AppConstants.PAN_PATTERN)) {
            throw new ValidationException("Invalid PAN format. Expected format: ABCDE1234F");
        }
    }

    public static boolean isValid(String pan) {
        return pan != null && pan.matches(AppConstants.PAN_PATTERN);
    }
}