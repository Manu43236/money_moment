package com.moneymoment.lending.controllers;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.dtos.ChangePasswordDto;
import com.moneymoment.lending.dtos.UpdateProfileDto;
import com.moneymoment.lending.dtos.UserResponseDto;
import com.moneymoment.lending.services.UserService;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile(Principal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getMyProfile(principal.getName()), "Profile fetched successfully"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateMyProfile(
            Principal principal,
            @RequestBody UpdateProfileDto request) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.updateMyProfile(principal.getName(), request), "Profile updated successfully"));
    }

    @PutMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Principal principal,
            @RequestBody ChangePasswordDto request) {
        userService.changeMyPassword(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }
}
