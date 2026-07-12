package com.verbamind.admin.dto;

import com.verbamind.payment.entity.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminPaymentResponse(
        UUID id,
        String organizationName,
        String planName,
        long amountPaise,
        String currency,
        PaymentStatus status,
        Instant createdAt
) {}
