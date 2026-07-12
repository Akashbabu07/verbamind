package com.verbamind.admin.controller;

import com.verbamind.admin.dto.AiUsageAnalyticsResponse;
import com.verbamind.admin.service.AdminAnalyticsService;
import com.verbamind.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping("/ai-usage")
    public ResponseEntity<ApiResponse<AiUsageAnalyticsResponse>> aiUsage() {
        return ResponseEntity.ok(ApiResponse.success(adminAnalyticsService.getAnalytics()));
    }
}