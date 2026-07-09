package com.verbamind.user.controller;

import com.verbamind.common.dto.ApiResponse;
import com.verbamind.security.CustomUserDetails;
import com.verbamind.user.dto.*;
import com.verbamind.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserProfile(currentUser.getId())));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateUserProfile(currentUser.getId(), request), "Profile updated"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed. Please log in again."));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteUser(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Account deleted"));
    }

    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionSummaryResponse>> getSubscription(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(userService.getSubscriptionSummary(currentUser.getId())));
    }

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> getUsage(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUsageSummary(currentUser.getId())));
    }
}