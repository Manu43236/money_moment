package com.moneymoment.lending.services;

import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.UserLoginAuditResponseDto;
import com.moneymoment.lending.entities.UserLoginAuditEntity;
import com.moneymoment.lending.repos.UserLoginAuditRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginAuditService {

    private final UserLoginAuditRepository auditRepository;

    LoginAuditService(UserLoginAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long userId, String username, String ipAddress) {
        UserLoginAuditEntity audit = new UserLoginAuditEntity();
        audit.setUserId(userId);
        audit.setUsername(username);
        audit.setLoginAt(LocalDateTime.now());
        audit.setIpAddress(ipAddress);
        audit.setStatus("SUCCESS");
        auditRepository.save(audit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username, String ipAddress, String reason) {
        UserLoginAuditEntity audit = new UserLoginAuditEntity();
        audit.setUsername(username);
        audit.setLoginAt(LocalDateTime.now());
        audit.setIpAddress(ipAddress);
        audit.setStatus("FAILED");
        audit.setFailureReason(reason);
        auditRepository.save(audit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogout(String username) {
        auditRepository.findTopByUsernameAndStatusOrderByLoginAtDesc(username, "SUCCESS")
                .ifPresent(audit -> {
                    audit.setLogoutAt(LocalDateTime.now());
                    auditRepository.save(audit);
                });
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserLoginAuditResponseDto> getLoginHistory(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("loginAt").descending());
        return PagedResponse.of(auditRepository.findAll(pageable).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserLoginAuditResponseDto> getLoginHistoryByUsername(String username, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("loginAt").descending());
        return PagedResponse.of(auditRepository.findByUsername(username, pageable).map(this::toDto));
    }

    private UserLoginAuditResponseDto toDto(UserLoginAuditEntity entity) {
        UserLoginAuditResponseDto dto = new UserLoginAuditResponseDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setUsername(entity.getUsername());
        dto.setLoginAt(entity.getLoginAt());
        dto.setLogoutAt(entity.getLogoutAt());
        dto.setIpAddress(entity.getIpAddress());
        dto.setStatus(entity.getStatus());
        dto.setFailureReason(entity.getFailureReason());
        return dto;
    }
}
