package com.verbamind.document.service;

import com.verbamind.organization.entity.Membership;
import com.verbamind.organization.entity.MembershipStatus;
import com.verbamind.organization.entity.OrganizationRole;
import com.verbamind.organization.exception.InsufficientRoleException;
import com.verbamind.organization.exception.NotAMemberException;
import com.verbamind.organization.repository.MembershipRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrganizationAccessGuard {

    private final MembershipRepository membershipRepository;

    public OrganizationAccessGuard(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Membership requireMembership(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);
    }

    public Membership requireRole(UUID organizationId, UUID userId, OrganizationRole minimumRole) {
        Membership membership = requireMembership(organizationId, userId);
        boolean allowed = switch (minimumRole) {
            case MEMBER -> true;
            case ADMIN -> membership.getRole() == OrganizationRole.ADMIN || membership.getRole() == OrganizationRole.OWNER;
            case OWNER -> membership.getRole() == OrganizationRole.OWNER;
        };
        if (!allowed) {
            throw new InsufficientRoleException();
        }
        return membership;
    }
}