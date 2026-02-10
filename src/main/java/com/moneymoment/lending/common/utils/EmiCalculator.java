package com.moneymoment.lending.common.utils;

public class EmiCalculator {
    
    /**
     * Calculate EMI using reducing balance method
     * Formula: EMI = [P × R × (1+R)^N] / [(1+R)^N - 1]
     * 
     * @param principal Loan amount
     * @param annualRate Annual interest rate (e.g., 12.5 for 12.5%)
     * @param tenureMonths Tenure in months
     * @return Monthly EMI amount
     */
    public static Double calculateEmi(Double principal, Double annualRate, Integer tenureMonths) {
        if (principal == null || annualRate == null || tenureMonths == null) {
            return 0.0;
        }
        
        if (principal <= 0 || annualRate <= 0 || tenureMonths <= 0) {
            return 0.0;
        }
        
        // Convert annual rate to monthly rate (divide by 12 and then by 100)
        Double monthlyRate = annualRate / 12 / 100;
        
        // Calculate (1 + R)^N
        Double onePlusRPowerN = Math.pow(1 + monthlyRate, tenureMonths);
        
        // Calculate EMI
        Double emi = (principal * monthlyRate * onePlusRPowerN) / (onePlusRPowerN - 1);
        
        // Round to 2 decimal places
        return Math.round(emi * 100.0) / 100.0;
    }
    
    /**
     * Calculate total interest payable
     */
    public static Double calculateTotalInterest(Double principal, Double emi, Integer tenureMonths) {
        Double totalAmount = emi * tenureMonths;
        return Math.round((totalAmount - principal) * 100.0) / 100.0;
    }
    
    /**
     * Calculate total amount payable (principal + interest)
     */
    public static Double calculateTotalAmount(Double principal, Double emi, Integer tenureMonths) {
        return Math.round((emi * tenureMonths) * 100.0) / 100.0;
    }
}