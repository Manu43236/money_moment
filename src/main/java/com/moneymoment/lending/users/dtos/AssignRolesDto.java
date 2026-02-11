package com.moneymoment.lending.users.dtos;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRolesDto {
    private Set<String> roleCodes;
}