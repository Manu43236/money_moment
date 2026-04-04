package com.moneymoment.lending.controllers;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.LoginRequestDto;
import com.moneymoment.lending.dtos.LoginResponseDto;
import com.moneymoment.lending.dtos.UserLoginAuditResponseDto;
import com.moneymoment.lending.services.LoginAuditService;
import com.moneymoment.lending.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final LoginAuditService loginAuditService;

    public AuthController(UserService userService, LoginAuditService loginAuditService) {
        this.userService = userService;
        this.loginAuditService = loginAuditService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(userService.login(request, ip), "Login successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            userService.logout(auth.getName());
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/login-history")
    public ResponseEntity<ApiResponse<PagedResponse<UserLoginAuditResponseDto>>> getLoginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(loginAuditService.getLoginHistory(page, size), "Login history fetched"));
    }

    @GetMapping("/login-history/{username}")
    public ResponseEntity<ApiResponse<PagedResponse<UserLoginAuditResponseDto>>> getLoginHistoryByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                loginAuditService.getLoginHistoryByUsername(username, page, size),
                "Login history fetched for " + username));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim(); // take first IP if proxy chain
        }
        return ip;
    }
}
