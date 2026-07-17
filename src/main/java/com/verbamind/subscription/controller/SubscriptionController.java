package com.verbamind.subscription.controller;

import com.verbamind.common.dto.ApiResponse;
import com.verbamind.security.CustomUserDetails;
import com.verbamind.subscription.dto.*;
import com.verbamind.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/api/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.listPlans()));
    }

    @GetMapping("/api/organizations/{organizationId}/subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> get(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(
                subscriptionService.getSubscription(currentUser.getId(), organizationId)));
    }

    @PostMapping("/api/organizations/{organizationId}/subscription/upgrade")
    public ResponseEntity<ApiResponse<UpgradeResultResponse>> upgrade(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @Valid @RequestBody UpgradePlanRequest request) {
        var result = subscriptionService.requestUpgrade(currentUser.getId(), organizationId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/api/organizations/{organizationId}/subscription/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId) {
        var sub = subscriptionService.cancelPlan(currentUser.getId(), organizationId);
        return ResponseEntity.ok(ApiResponse.success(sub, "Subscription will be canceled at period end"));
    }
}