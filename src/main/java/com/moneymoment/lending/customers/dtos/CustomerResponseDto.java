package com.moneymoment.lending.customers.dtos;

import java.time.LocalDateTime;

import com.moneymoment.lending.common.enums.EmploymentType;
import com.moneymoment.lending.customers.entities.CustomerEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponseDto {
    private Long id;
    private String name;
    private String phone;
    private LocalDateTime dob; // ISO format: YYYY-MM-DD
    private String email;
    private String pan;
    private String aadhar;
    private String address;
    private String occupation;
    private String customerNumber;
    private EmploymentType employmentType; // "SALARIED" or "SELF_EMPLOYED"
    private Double monthlySalary;
    // Getters and Setters


    public CustomerResponseDto fromEntityToDto(CustomerEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.phone = entity.getPhone();
        this.dob = entity.getDob();
        this.email = entity.getEmail();
        this.pan = entity.getPan();
        this.aadhar = entity.getAadhar();
        this.address = entity.getAddress();
        this.occupation = entity.getOccupation();
        this.employmentType = entity.getEmploymentType();
        this.monthlySalary = entity.getMonthlySalary();

        return this;
    }
}
