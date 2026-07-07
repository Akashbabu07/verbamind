package com.verbamind.auth.dto;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        String fullName,
        boolean emailVerified
) {}