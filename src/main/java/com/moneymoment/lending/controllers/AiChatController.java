package com.moneymoment.lending.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AiChatResponseDto>> chat(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) MultipartFile panImage,
            @RequestParam(required = false) MultipartFile aadhaarImage,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails != null ? userDetails.getUsername() : "system";
        AiChatResponseDto response = aiChatService.chat(sessionId, message, panImage, aadhaarImage, username);
        return ResponseEntity.ok(ApiResponse.success(response, "OK"));
    }
}
