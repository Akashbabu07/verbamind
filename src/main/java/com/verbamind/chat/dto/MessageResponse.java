package com.verbamind.chat.dto;

import com.verbamind.ai.dto.CitationDto;
import com.verbamind.chat.entity.MessageRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        MessageRole role,
        String content,
        List<CitationDto> citations,
        Instant createdAt
) {}