package com.moneymoment.lending.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserLoginAuditResponseDto {
    private Long id;
    private Long userId;
    private String username;
    private LocalDateTime loginAt;
    private LocalDateTime logoutAt;
    private String ipAddress;
    private String status;
    private String failureReason;
}
