package com.moneymoment.lending.customers.dtos;

import java.time.LocalDateTime;

import com.moneymoment.lending.common.enums.EmploymentType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDto {
    private String name;
    private String phone;
    private LocalDateTime dob; // ISO format: YYYY-MM-DD
    private String email;
    private String pan;
    private String aadhar;
    private String address;
    private String occupation;
    private EmploymentType employmentType; // "SALARIED" or "SELF_EMPLOYED"
 private Double monthlySalary;
    // Getters and Setters
}
