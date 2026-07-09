package com.verbamind.organization.dto;

import com.verbamind.organization.entity.MembershipStatus;
import com.verbamind.organization.entity.OrganizationRole;

import java.util.UUID;

public record MembershipResponse(
        UUID membershipId,
        UUID userId,
        String email,
        String fullName,
        OrganizationRole role,
        MembershipStatus status
) {}