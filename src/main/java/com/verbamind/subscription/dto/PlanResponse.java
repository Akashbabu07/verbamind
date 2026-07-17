package com.verbamind.subscription.dto;

import com.verbamind.subscription.entity.PlanCode;

import java.util.UUID;

public record PlanResponse(
        UUID id,
        PlanCode code,
        String name,
        long storageLimitBytes,
        int dailyAiQuestionLimit,
        int monthlyAiQuestionLimit,
        long maxUploadSizeBytes,
        long priceMonthlyPaise
) {}