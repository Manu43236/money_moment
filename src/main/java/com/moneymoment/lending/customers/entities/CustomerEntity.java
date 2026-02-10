package com.moneymoment.lending.customers.entities;

import java.time.LocalDateTime;

import com.moneymoment.lending.common.enums.EmploymentType;
import com.moneymoment.lending.common.utils.NumberGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String customerNumber = NumberGenerator.generateCustomerNumber();

    @Column
    private String phone;

    @Column
    private LocalDateTime dob;

    @Column(nullable = false, unique = true)
    private String email;

    // for KYC
    @Column(nullable = false, length = 10)
    private String pan;

    @Column(nullable = false, length = 12)
    private String aadhar;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    // employment details
    @Column(name = "occupation")
    private String occupation;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Column(name = "monthly_salary")
    private Double monthlySalary;

    @Column(name = "credit_score", nullable = false)
    private Double creditScore = 758.5; // default value, can be updated based on credit checks

    // audit fields
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private String createdBy;

    @Column
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
