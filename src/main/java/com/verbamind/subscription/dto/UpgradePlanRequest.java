package com.verbamind.subscription.dto;

import com.verbamind.subscription.entity.PlanCode;
import jakarta.validation.constraints.NotNull;

public record UpgradePlanRequest(
        @NotNull PlanCode planCode
) {}