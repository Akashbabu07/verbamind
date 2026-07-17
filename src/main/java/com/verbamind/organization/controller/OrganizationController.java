package com.verbamind.organization.controller;

import com.verbamind.common.dto.ApiResponse;
import com.verbamind.organization.dto.*;
import com.verbamind.organization.service.OrganizationService;
import com.verbamind.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateOrganizationRequest request) {
        var org = organizationService.createOrganization(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(org, "Organization created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> listMine(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.listMyOrganizations(currentUser.getId())));
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> get(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(
                organizationService.getOrganization(currentUser.getId(), organizationId)));
    }

    @PostMapping("/{organizationId}/members/invite")
    public ResponseEntity<ApiResponse<MembershipResponse>> invite(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @Valid @RequestBody InviteMemberRequest request) {
        var membership = organizationService.inviteMember(currentUser.getId(), organizationId, request);
        return ResponseEntity.ok(ApiResponse.success(membership, "Invite sent"));
    }

    @PostMapping("/members/accept-invite")
    public ResponseEntity<ApiResponse<MembershipResponse>> acceptInvite(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody AcceptInviteRequest request) {
        var membership = organizationService.acceptInvite(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(membership, "Invite accepted"));
    }

    @GetMapping("/{organizationId}/members")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> listMembers(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(
                organizationService.listMembers(currentUser.getId(), organizationId)));
    }

    @PatchMapping("/{organizationId}/members/{membershipId}/role")
    public ResponseEntity<ApiResponse<MembershipResponse>> updateRole(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        var membership = organizationService.updateMemberRole(
                currentUser.getId(), organizationId, membershipId, request);
        return ResponseEntity.ok(ApiResponse.success(membership, "Role updated"));
    }

    @DeleteMapping("/{organizationId}/members/{membershipId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId) {
        organizationService.removeMember(currentUser.getId(), organizationId, membershipId);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed"));
    }
}