package com.verbamind.payment.dto;

import com.verbamind.subscription.entity.PlanCode;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull PlanCode planCode
) {}