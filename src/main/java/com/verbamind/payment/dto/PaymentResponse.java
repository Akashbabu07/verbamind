package com.verbamind.payment.dto;

import com.verbamind.payment.entity.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String planName,
        long amountPaise,
        String currency,
        PaymentStatus status,
        Instant createdAt
) {}