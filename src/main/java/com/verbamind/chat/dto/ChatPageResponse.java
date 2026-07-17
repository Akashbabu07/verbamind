package com.verbamind.chat.dto;

import java.util.List;

public record ChatPageResponse(
        List<ChatResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {}