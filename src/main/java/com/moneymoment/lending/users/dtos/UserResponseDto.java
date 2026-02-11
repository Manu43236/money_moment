package com.moneymoment.lending.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    // Identifiers
    private Long id;
    private String userNumber;
    private String employeeId;
    private String username;
    private String email;

    // Personal Info
    private String fullName;
    private String phone;

    // Work Info
    private String department;
    private String designation;
    private String branchCode;
    private String regionCode;

    // Manager Info
    private Long managerId;
    private String managerName;
    private String managerEmployeeId;

    // Dates
    private LocalDate joiningDate;
    private Boolean isActive;

    // Roles
    private Set<RoleResponseDto> roles;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}