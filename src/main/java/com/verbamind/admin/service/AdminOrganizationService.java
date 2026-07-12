package com.verbamind.admin.service;

import com.verbamind.admin.dto.*;
import com.verbamind.organization.entity.Organization;
import com.verbamind.organization.exception.OrganizationNotFoundException;
import com.verbamind.organization.repository.MembershipRepository;
import com.verbamind.organization.repository.OrganizationRepository;
import com.verbamind.subscription.repository.SubscriptionRepository;
import com.verbamind.subscription.service.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    public AdminOrganizationService(OrganizationRepository organizationRepository,
                                    MembershipRepository membershipRepository,
                                    SubscriptionRepository subscriptionRepository,
                                    SubscriptionService subscriptionService) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    public AdminOrganizationPageResponse listOrganizations(Pageable pageable) {
        Page<Organization> page = organizationRepository.findAll(pageable);
        var items = page.getContent().stream().map(this::toResponse).toList();
        return new AdminOrganizationPageResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public AdminOrganizationResponse getOrganization(UUID organizationId) {
        return toResponse(getOrgOrThrow(organizationId));
    }

    /**
     * Admin override — force-switches an org's plan without requiring
     * payment (e.g. comping a customer, fixing a stuck subscription after a
     * support ticket). Distinct from the normal Razorpay-gated upgrade flow.
     */
    @Transactional
    public AdminOrganizationResponse overridePlan(UUID organizationId, AdminSubscriptionOverrideRequest request) {
        getOrgOrThrow(organizationId); // validates existence
        subscriptionService.changePlan(organizationId, request.planCode());
        return toResponse(getOrgOrThrow(organizationId));
    }

    private Organization getOrgOrThrow(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
    }

    private AdminOrganizationResponse toResponse(Organization org) {
        long memberCount = membershipRepository.countByOrganizationId(org.getId());
        String planCode = subscriptionRepository.findByOrganizationId(org.getId())
                .map(sub -> sub.getPlan().getCode().name())
                .orElse("NONE");

        return new AdminOrganizationResponse(org.getId(), org.getName(), org.getSlug(), org.isPersonal(),
                org.getOwner().getId(), org.getOwner().getEmail(), planCode, memberCount, org.getCreatedAt());
    }
}