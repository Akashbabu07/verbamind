package com.verbamind.document.service;

import com.verbamind.organization.entity.Membership;
import com.verbamind.organization.entity.MembershipStatus;
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
}