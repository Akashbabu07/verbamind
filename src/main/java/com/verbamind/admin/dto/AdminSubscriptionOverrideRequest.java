package com.verbamind.admin.dto;

import com.verbamind.subscription.entity.PlanCode;
import jakarta.validation.constraints.NotNull;

public record AdminSubscriptionOverrideRequest(
        @NotNull PlanCode planCode
) {}