package com.verbamind.document.dto;

import java.util.List;

public record DocumentPageResponse(
        List<DocumentResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {}