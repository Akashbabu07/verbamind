package com.verbamind.usage.dto;

public record UsageResponse(
        long aiRequestsToday,
        long aiRequestsThisMonth,
        long tokensUsedThisMonth,
        long storageUsedBytes,
        long documentsUploaded,
        int dailyAiQuestionLimit,
        int monthlyAiQuestionLimit,
        long storageLimitBytes
) {}