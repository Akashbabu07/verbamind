package com.verbamind.admin.dto;

import java.util.List;

public record AdminPaymentPageResponse(
        List<AdminPaymentResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {}