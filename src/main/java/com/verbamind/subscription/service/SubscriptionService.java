package com.verbamind.subscription.service;

import com.verbamind.organization.entity.Organization;
import com.verbamind.organization.entity.OrganizationRole;
import com.verbamind.organization.repository.OrganizationRepository;
import com.verbamind.document.service.OrganizationAccessGuard;
import com.verbamind.subscription.dto.*;
import com.verbamind.subscription.entity.Plan;
import com.verbamind.subscription.entity.PlanCode;
import com.verbamind.subscription.entity.Subscription;
import com.verbamind.subscription.entity.SubscriptionStatus;
import com.verbamind.subscription.exception.PlanNotFoundException;
import com.verbamind.subscription.exception.SubscriptionNotFoundException;
import com.verbamind.subscription.repository.PlanRepository;
import com.verbamind.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationAccessGuard accessGuard;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               PlanRepository planRepository,
                               OrganizationRepository organizationRepository,
                               OrganizationAccessGuard accessGuard) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.organizationRepository = organizationRepository;
        this.accessGuard = accessGuard;
    }
    @Transactional
    public void createFreeSubscription(Organization organization) {
        Plan freePlan = planRepository.findByCode(PlanCode.FREE)
                .orElseThrow(() -> new PlanNotFoundException("FREE plan not seeded"));

        Subscription subscription = new Subscription();
        subscription.setOrganization(organization);
        subscription.setPlan(freePlan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(Instant.now());
        subscription.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
        subscriptionRepository.save(subscription);
    }

    public List<PlanResponse> listPlans() {
        return planRepository.findAll().stream()
                .filter(Plan::isActive)
                .map(this::toPlanResponse)
                .toList();
    }

    public SubscriptionResponse getSubscription(UUID currentUserId, UUID organizationId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Subscription sub = getSubscriptionOrThrow(organizationId);
        return toSubscriptionResponse(sub);
    }


    @Transactional
    public SubscriptionResponse changePlan(UUID organizationId, PlanCode planCode) {
        Subscription sub = getSubscriptionOrThrow(organizationId);
        Plan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> new PlanNotFoundException("Plan not found: " + planCode));

        sub.setPlan(plan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCancelAtPeriodEnd(false);
        sub.setCurrentPeriodStart(Instant.now());
        sub.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
        subscriptionRepository.save(sub);

        return toSubscriptionResponse(sub);
    }

    @Transactional
    public UpgradeResultResponse requestUpgrade(UUID currentUserId, UUID organizationId, UpgradePlanRequest request) {
        accessGuard.requireRole(organizationId, currentUserId, OrganizationRole.ADMIN);

        if (request.planCode() == PlanCode.FREE) {
            SubscriptionResponse sub = changePlan(organizationId, PlanCode.FREE);
            return new UpgradeResultResponse(false, sub, null);
        }

        Plan plan = planRepository.findByCode(request.planCode())
                .orElseThrow(() -> new PlanNotFoundException("Plan not found: " + request.planCode()));

        return new UpgradeResultResponse(true, null, plan.getCode());
    }
    @Transactional
    public SubscriptionResponse cancelPlan(UUID currentUserId, UUID organizationId) {
        accessGuard.requireRole(organizationId, currentUserId, OrganizationRole.ADMIN);
        Subscription sub = getSubscriptionOrThrow(organizationId);
        sub.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(sub);
        return toSubscriptionResponse(sub);
    }


    public Plan getActivePlan(UUID organizationId) {
        return getSubscriptionOrThrow(organizationId).getPlan();
    }

    private Subscription getSubscriptionOrThrow(UUID organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new SubscriptionNotFoundException("No subscription found for organization"));
    }

    private PlanResponse toPlanResponse(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getCode(), plan.getName(),
                plan.getStorageLimitBytes(), plan.getDailyAiQuestionLimit(),
                plan.getMonthlyAiQuestionLimit(), plan.getMaxUploadSizeBytes(),
                plan.getPriceMonthlyPaise());
    }

    private SubscriptionResponse toSubscriptionResponse(Subscription sub) {
        return new SubscriptionResponse(sub.getId(), toPlanResponse(sub.getPlan()), sub.getStatus(),
                sub.getCurrentPeriodStart(), sub.getCurrentPeriodEnd(), sub.isCancelAtPeriodEnd());
    }
}