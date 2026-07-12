package com.verbamind.admin.dto;

public record AiUsageAnalyticsResponse(
        long totalOrganizations,
        long totalAiRequestsToday,
        long totalAiRequestsThisMonth,
        long totalTokensThisMonth,
        long totalDocumentsUploaded,
        long totalStorageUsedBytes
) {}