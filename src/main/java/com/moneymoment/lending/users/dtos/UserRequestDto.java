package com.moneymoment.lending.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {

    private String employeeId;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;
    private String department;
    private String designation;
    private String branchCode;
    private String regionCode;
    private String managerEmployeeId; // Manager's employee ID
    private LocalDate joiningDate;
    private Set<String> roleCodes; // e.g., ["CREDIT_MANAGER", "LOAN_OFFICER"]
}