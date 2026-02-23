package com.moneymoment.lending.entities;

import java.time.LocalDate;

import com.moneymoment.lending.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "collateral_details")
public class CollateralDetailsEntity extends BaseEntity {

    @Column(name = "collateral_number", unique = true, nullable = false, length = 50)
    private String collateralNumber;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    // Collateral Type
    @Column(name = "collateral_type", nullable = false, length = 50)
    private String collateralType;

    // Property Details
    @Column(name = "property_address", columnDefinition = "TEXT")
    private String propertyAddress;

    @Column(name = "property_type", length = 50)
    private String propertyType;

    @Column(name = "property_area_sqft")
    private Double propertyAreaSqft;

    @Column(name = "property_value")
    private Double propertyValue;

    // Vehicle Details
    @Column(name = "vehicle_registration_number", length = 20)
    private String vehicleRegistrationNumber;

    @Column(name = "vehicle_make", length = 50)
    private String vehicleMake;

    @Column(name = "vehicle_model", length = 50)
    private String vehicleModel;

    @Column(name = "vehicle_year")
    private Integer vehicleYear;

    @Column(name = "vehicle_value")
    private Double vehicleValue;

    // Gold Details
    @Column(name = "gold_weight_grams")
    private Double goldWeightGrams;

    @Column(name = "gold_purity", length = 20)
    private String goldPurity;

    @Column(name = "gold_item_description", columnDefinition = "TEXT")
    private String goldItemDescription;

    @Column(name = "gold_value")
    private Double goldValue;

    // Valuation
    @Column(name = "valuation_amount", nullable = false)
    private Double valuationAmount;

    @Column(name = "valuation_date")
    private LocalDate valuationDate;

    @Column(name = "valuator_name", length = 100)
    private String valuatorName;

    @Column(name = "valuator_certificate_number", length = 50)
    private String valuatorCertificateNumber;

    // LTV Calculation
    @Column(name = "loan_amount", nullable = false)
    private Double loanAmount;

    @Column(name = "ltv_percentage", nullable = false)
    private Double ltvPercentage;

    // Status
    @Column(name = "collateral_status", nullable = false, length = 20)
    private String collateralStatus;

    @Column(name = "pledge_date")
    private LocalDate pledgeDate;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    // Remarks
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}