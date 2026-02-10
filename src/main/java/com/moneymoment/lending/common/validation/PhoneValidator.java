package com.moneymoment.lending.common.validation;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.exception.ValidationException;

public class PhoneValidator {
    
    public static void validate(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException("Phone number is required");
        }
        
        if (!phone.matches(AppConstants.PHONE_PATTERN)) {
            throw new ValidationException("Invalid phone number. Must be 10 digits starting with 6-9");
        }
    }
    
    public static boolean isValid(String phone) {
        return phone != null && phone.matches(AppConstants.PHONE_PATTERN);
    }
}