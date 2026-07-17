package com.verbamind.admin.dto;

import com.verbamind.auth.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        boolean emailVerified,
        boolean enabled,
        boolean deleted,
        Instant createdAt
) {}