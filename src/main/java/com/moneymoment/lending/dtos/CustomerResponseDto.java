package com.moneymoment.lending.dtos;

import java.time.LocalDateTime;

import com.moneymoment.lending.common.enums.EmploymentType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponseDto {
    private Long id;
    private String customerNumber;
    private String name;
    private String phone;
    private LocalDateTime dob;
    private String email;
    private String pan;
    private String aadhar;
    private String address;
    private String occupation;
    private EmploymentType employmentType;
    private Double monthlySalary;
    private String homeBranchCode;
    private String relationshipManagerEmployeeId;
    private String relationshipManagerName;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
