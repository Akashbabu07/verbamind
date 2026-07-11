package com.verbamind.payment.dto;

public record CreateOrderResponse(
        String razorpayOrderId,
        String razorpayKeyId,
        long amountPaise,
        String currency
) {}