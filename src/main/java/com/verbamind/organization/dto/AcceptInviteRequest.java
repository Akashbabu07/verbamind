package com.verbamind.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequest(@NotBlank String token) {}