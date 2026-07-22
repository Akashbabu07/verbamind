package com.verbamind.document.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID id, int versionNumber, long fileSize, Instant createdAt, boolean isCurrent
) {}