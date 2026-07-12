package com.verbamind.admin.dto;

import java.util.List;

public record AdminOrganizationPageResponse(
        List<AdminOrganizationResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {}