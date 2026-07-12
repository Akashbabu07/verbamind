package com.verbamind.admin.controller;

import com.verbamind.admin.dto.*;
import com.verbamind.admin.service.AdminOrganizationService;
import com.verbamind.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/organizations")
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;

    public AdminOrganizationController(AdminOrganizationService adminOrganizationService) {
        this.adminOrganizationService = adminOrganizationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminOrganizationPageResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOrganizationService.listOrganizations(PageRequest.of(page, size))));
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<ApiResponse<AdminOrganizationResponse>> get(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(adminOrganizationService.getOrganization(organizationId)));
    }

    @PatchMapping("/{organizationId}/plan")
    public ResponseEntity<ApiResponse<AdminOrganizationResponse>> overridePlan(
            @PathVariable UUID organizationId,
            @Valid @RequestBody AdminSubscriptionOverrideRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOrganizationService.overridePlan(organizationId, request), "Plan overridden"));
    }
}