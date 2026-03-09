package com.moneymoment.lending.dtos;

import lombok.Data;

@Data
public class UpdateProfileDto {
    private String fullName;
    private String phone;
    private String email;
}
