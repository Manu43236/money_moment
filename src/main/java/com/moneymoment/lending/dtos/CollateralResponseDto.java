package com.moneymoment.lending.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollateralResponseDto {

    // Identifiers
    private Long id;
    private String collateralNumber;

    // Loan Info
    private Long loanId;
    private String loanNumber;
    private Double loanAmount;

    // Customer Info
    private Long customerId;
    private String customerNumber;
    private String customerName;

    // Collateral Type
    private String collateralType;

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

    // LTV
    private Double ltvPercentage;
    private Double maxAllowedLtv;

    // Status
    private String collateralStatus;
    private LocalDate pledgeDate;
    private LocalDate releaseDate;

    // Remarks
    private String remarks;

    // Audit
    private LocalDateTime createdAt;
}