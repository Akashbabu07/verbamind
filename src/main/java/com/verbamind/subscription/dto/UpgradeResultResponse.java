package com.verbamind.subscription.dto;

import com.verbamind.subscription.entity.PlanCode;

public record UpgradeResultResponse(
        boolean requiresPayment,
        SubscriptionResponse subscription,
        PlanCode pendingPlanCode
) {}