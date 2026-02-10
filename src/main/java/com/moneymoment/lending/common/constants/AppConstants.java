package com.moneymoment.lending.common.constants;

public class AppConstants {
    
    // Loan Constants
    public static final Double MIN_LOAN_AMOUNT = 10000.0;
    public static final Double MAX_LOAN_AMOUNT = 50000000.0;
    public static final Integer MIN_TENURE_MONTHS = 6;
    public static final Integer MAX_TENURE_MONTHS = 300;
    public static final Double MIN_INTEREST_RATE = 7.0;
    public static final Double MAX_INTEREST_RATE = 24.0;
    
    // Credit Score Constants
    public static final Integer MIN_CREDIT_SCORE = 300;
    public static final Integer MAX_CREDIT_SCORE = 900;
    public static final Integer GOOD_CREDIT_SCORE = 750;
    
    // Eligibility Constants
    public static final Integer MIN_AGE = 21;
    public static final Integer MAX_AGE = 65;
    public static final Double MIN_MONTHLY_SALARY = 15000.0;
    public static final Double MAX_DTI_RATIO = 50.0; // Debt-to-Income ratio %
    
    // Regex Patterns
    public static final String PAN_PATTERN = "[A-Z]{5}[0-9]{4}[A-Z]{1}";
    public static final String AADHAAR_PATTERN = "[0-9]{12}";
    public static final String PHONE_PATTERN = "[6-9][0-9]{9}";
    public static final String IFSC_PATTERN = "^[A-Z]{4}0[A-Z0-9]{6}$";
    
    // Number Generation Prefixes
    public static final String CUSTOMER_NUMBER_PREFIX = "CUST";
    public static final String LOAN_NUMBER_PREFIX = "LN";

    //DOCUMENTS Generation Prefixes
    public static final String USER_DOCUMENT_NUMBER_PREFIX = "URDOC";
    public static final String LOAN_DOCUMENT_NUMBER_PREFIX = "LOANDOC";
    
    // System User
    public static final String SYSTEM_USER = "SYSTEM";
    
    // NPA Days
    public static final Integer NPA_DAYS = 90;
    public static final Integer GRACE_PERIOD_DAYS = 3;
    
    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}