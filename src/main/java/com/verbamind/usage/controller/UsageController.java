package com.verbamind.usage.controller;

import com.verbamind.common.dto.ApiResponse;
import com.verbamind.security.CustomUserDetails;
import com.verbamind.usage.dto.UsageResponse;
import com.verbamind.usage.service.UsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/organizations/{organizationId}/usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UsageResponse>> getUsage(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(
                usageService.getUsageSummary(currentUser.getId(), organizationId)));
    }
}