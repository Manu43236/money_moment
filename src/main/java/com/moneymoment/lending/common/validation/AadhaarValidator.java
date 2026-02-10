package com.moneymoment.lending.common.validation;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.exception.ValidationException;

public class AadhaarValidator {
    
    public static void validate(String aadhaar) {
        if (aadhaar == null || aadhaar.trim().isEmpty()) {
            throw new ValidationException("Aadhaar number is required");
        }
        
        if (!aadhaar.matches(AppConstants.AADHAAR_PATTERN)) {
            throw new ValidationException("Invalid Aadhaar format. Expected 12 digits");
        }
    }
    
    public static boolean isValid(String aadhaar) {
        return aadhaar != null && aadhaar.matches(AppConstants.AADHAAR_PATTERN);
    }
}