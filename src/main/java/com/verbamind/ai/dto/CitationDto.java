package com.verbamind.ai.dto;

import java.util.UUID;

public record CitationDto(
        int marker,
        UUID documentId,
        String fileName,
        int chunkIndex,
        String snippet
) {}