package com.verbamind.organization.dto;

import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        boolean personal,
        UUID ownerId,
        String currentUserRole
) {}