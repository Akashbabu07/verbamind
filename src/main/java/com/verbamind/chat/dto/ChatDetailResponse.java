package com.verbamind.chat.dto;

import java.util.List;
import java.util.UUID;

public record ChatDetailResponse(
        UUID id,
        String title,
        List<MessageResponse> messages
) {}