package com.verbamind.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminOrganizationResponse(
        UUID id,
        String name,
        String slug,
        boolean personal,
        UUID ownerId,
        String ownerEmail,
        String planCode,
        long memberCount,
        Instant createdAt
) {}