package com.verbamind.user.dto;

public record SubscriptionSummaryResponse(
        String planName,
        String status,
        long storageLimit,
        long dailyAiQuestionLimit,
        long monthlyAiQuestionLimit
) {}