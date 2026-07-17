package com.verbamind.user.dto;

public record UsageSummaryResponse(
        long aiRequestsToday,
        long aiRequestsThisMonth,
        long tokensUsedThisMonth,
        long storageUsedBytes,
        long documentsUploaded
) {}