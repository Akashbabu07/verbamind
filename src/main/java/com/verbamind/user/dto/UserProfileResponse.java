package com.verbamind.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        boolean emailVerified,
        Instant createdAt
) {}