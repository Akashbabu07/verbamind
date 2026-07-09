package com.verbamind.organization.dto;

import com.verbamind.organization.entity.OrganizationRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull OrganizationRole role
) {}