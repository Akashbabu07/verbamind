package com.verbamind.document.dto;

import com.verbamind.document.entity.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSize,
        DocumentStatus status,
        UUID ownerId,
        Instant createdAt
) {}