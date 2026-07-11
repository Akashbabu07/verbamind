package com.verbamind.subscription.dto;

import com.verbamind.subscription.entity.PlanCode;

public record UpgradeResultResponse(
        boolean requiresPayment,
        SubscriptionResponse subscription,   // populated only if switched immediately (FREE)
        PlanCode pendingPlanCode             // populated only if requiresPayment = true
) {}