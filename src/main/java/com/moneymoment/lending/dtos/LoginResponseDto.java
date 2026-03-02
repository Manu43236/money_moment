package com.moneymoment.lending.dtos;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private Long id;
    private String userNumber;
    private String employeeId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String department;
    private String designation;
    private String branchCode;
    private String regionCode;
    private Set<RoleResponseDto> roles;
    private String token;
}
