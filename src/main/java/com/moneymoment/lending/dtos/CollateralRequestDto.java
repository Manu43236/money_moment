package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollateralRequestDto {
    
    private String loanNumber;
    private String collateralType;  // PROPERTY, VEHICLE, GOLD
    
    // Property Details
    private String propertyAddress;
    private String propertyType;
    private Double propertyAreaSqft;
    private Double propertyValue;
    
    // Vehicle Details
    private String vehicleRegistrationNumber;
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;
    private Double vehicleValue;
    
    // Gold Details
    private Double goldWeightGrams;
    private String goldPurity;
    private String goldItemDescription;
    private Double goldValue;
    
    // Valuation
    private Double valuationAmount;
    private LocalDate valuationDate;
    private String valuatorName;
    private String valuatorCertificateNumber;
    
    // Remarks
    private String remarks;
}