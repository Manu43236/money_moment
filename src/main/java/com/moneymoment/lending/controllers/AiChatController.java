package com.moneymoment.lending.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.dtos.AiChatResponseDto;
import com.moneymoment.lending.services.AiChatService;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PreAuthorize("hasAnyAuthority('LOAN_OFFICER', 'ADMIN')")
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponseDto>> chat(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails != null ? userDetails.getUsername() : "system";
        String sessionId = body.get("sessionId");
        String message = body.get("message");

        Long customerId = null;
        if (body.get("customerId") != null) {
            try { customerId = Long.parseLong(body.get("customerId")); } catch (Exception ignored) {}
        }

        AiChatResponseDto response = aiChatService.chat(sessionId, message, username, customerId);
        return ResponseEntity.ok(ApiResponse.success(response, "OK"));
    }
}
