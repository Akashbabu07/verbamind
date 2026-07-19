package com.verbamind.organization.service;

import com.verbamind.auth.entity.User;
import com.verbamind.auth.repository.UserRepository;
import com.verbamind.auth.service.EmailService;
import com.verbamind.organization.dto.*;
import com.verbamind.organization.entity.Membership;
import com.verbamind.organization.entity.MembershipStatus;
import com.verbamind.organization.entity.Organization;
import com.verbamind.organization.entity.OrganizationRole;
import com.verbamind.organization.exception.*;
import com.verbamind.organization.repository.MembershipRepository;
import com.verbamind.organization.repository.OrganizationRepository;
import com.verbamind.subscription.service.SubscriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class OrganizationService {
    private final SubscriptionService subscriptionService;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               MembershipRepository membershipRepository,
                               UserRepository userRepository,
                               EmailService emailService,
                               SubscriptionService subscriptionService) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.subscriptionService = subscriptionService;
    }

    public Organization getPersonalWorkspace(UUID userId) {
        return membershipRepository.findByUserId(userId).stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .map(Membership::getOrganization)
                .filter(Organization::isPersonal)
                .findFirst()
                .orElseThrow(() -> new OrganizationNotFoundException("Personal workspace not found for user"));
    }

    @Transactional
    public Organization createPersonalWorkspace(User user) {
        Organization org = new Organization();
        org.setName(user.getFullName() + " Workspace");
        org.setSlug(generateUniqueSlug(user.getFullName() + " workspace"));
        org.setPersonal(true);
        org.setOwner(user);
        organizationRepository.save(org);
        subscriptionService.createFreeSubscription(org);

        Membership membership = new Membership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(OrganizationRole.OWNER);
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        return org;
    }

    @Transactional
    public OrganizationResponse createOrganization(UUID currentUserId, CreateOrganizationRequest request) {
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new OrganizationNotFoundException("User not found"));

        Organization org = new Organization();
        org.setName(request.name());
        org.setSlug(generateUniqueSlug(request.name()));
        org.setPersonal(false);
        org.setOwner(owner);
        organizationRepository.save(org);
        subscriptionService.createFreeSubscription(org);

        Membership membership = new Membership();
        membership.setOrganization(org);
        membership.setUser(owner);
        membership.setRole(OrganizationRole.OWNER);
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        return toResponse(org, OrganizationRole.OWNER);
    }

    public OrganizationResponse getOrganization(UUID currentUserId, UUID organizationId) {
        Organization org = getOrgOrThrow(organizationId);
        Membership membership = requireMembership(organizationId, currentUserId);
        return toResponse(org, membership.getRole());
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listMyOrganizations(UUID currentUserId) {
        return membershipRepository.findByUserId(currentUserId).stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .map(m -> toResponse(m.getOrganization(), m.getRole()))
                .toList();
    }

    @Transactional
    public MembershipResponse inviteMember(UUID currentUserId, UUID organizationId, InviteMemberRequest request) {
        Organization org = getOrgOrThrow(organizationId);
        requireRole(organizationId, currentUserId, OrganizationRole.ADMIN);

        userRepository.findByEmail(request.email()).ifPresent(existingUser -> {
            if (membershipRepository.existsByOrganizationIdAndUserId(organizationId, existingUser.getId())) {
                throw new AlreadyMemberException(request.email());
            }
        });

        Membership membership = new Membership();
        membership.setOrganization(org);
        membership.setInvitedEmail(request.email());
        membership.setRole(request.role());
        membership.setStatus(MembershipStatus.PENDING);
        membership.setInviteToken(UUID.randomUUID().toString());
        userRepository.findByEmail(request.email()).ifPresent(membership::setUser);

        membershipRepository.save(membership);

        emailService.sendOrganizationInviteEmail(request.email(), org.getName(), membership.getInviteToken());

        return toMembershipResponse(membership);
    }

    @Transactional
    public MembershipResponse acceptInvite(UUID currentUserId, AcceptInviteRequest request) {
        Membership membership = membershipRepository.findByInviteToken(request.token())
                .orElseThrow(() -> new InvalidInviteException("Invalid or expired invite token"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new InvalidInviteException("User not found"));

        if (membership.getInvitedEmail() != null
                && !membership.getInvitedEmail().equalsIgnoreCase(currentUser.getEmail())) {
            throw new InvalidInviteException("This invite was sent to a different email address");
        }

        membership.setUser(currentUser);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setInviteToken(null);
        membershipRepository.save(membership);

        return toMembershipResponse(membership);
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> listMembers(UUID currentUserId, UUID organizationId) {
        getOrgOrThrow(organizationId);
        requireMembership(organizationId, currentUserId);

        return membershipRepository.findByOrganizationId(organizationId).stream()
                .map(this::toMembershipResponse)
                .toList();
    }

    @Transactional
    public MembershipResponse updateMemberRole(UUID currentUserId, UUID organizationId,
                                               UUID membershipId, UpdateMemberRoleRequest request) {
        requireRole(organizationId, currentUserId, OrganizationRole.ADMIN);

        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new OrganizationNotFoundException("Membership not found"));

        if (membership.getRole() == OrganizationRole.OWNER || request.role() == OrganizationRole.OWNER) {
            // Ownership can't be granted or taken away via this ADMIN-level endpoint: it would let
            // an ADMIN self-promote to OWNER (or create a second OWNER), which is a privilege
            // escalation. Ownership transfer needs its own dedicated, owner-only flow.
            throw new InsufficientRoleException();
        }

        membership.setRole(request.role());
        membershipRepository.save(membership);
        return toMembershipResponse(membership);
    }

    @Transactional
    public void removeMember(UUID currentUserId, UUID organizationId, UUID membershipId) {
        requireRole(organizationId, currentUserId, OrganizationRole.ADMIN);

        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new OrganizationNotFoundException("Membership not found"));

        if (membership.getRole() == OrganizationRole.OWNER) {
            throw new InsufficientRoleException(); // can't remove the owner
        }

        membershipRepository.delete(membership);
    }


    private Organization getOrgOrThrow(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
    }

    private Membership requireMembership(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);
    }

    private void requireRole(UUID organizationId, UUID userId, OrganizationRole minimumRole) {
        Membership membership = requireMembership(organizationId, userId);
        boolean allowed = switch (minimumRole) {
            case MEMBER -> true; // any active member
            case ADMIN -> membership.getRole() == OrganizationRole.ADMIN || membership.getRole() == OrganizationRole.OWNER;
            case OWNER -> membership.getRole() == OrganizationRole.OWNER;
        };
        if (!allowed) {
            throw new InsufficientRoleException();
        }
    }

    private String generateUniqueSlug(String base) {
        String slugBase = slugify(base);
        String slug = slugBase;
        int suffix = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = slugBase + "-" + suffix++;
        }
        return slug;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
        String slug = normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        return Pattern.compile("-+").matcher(slug).replaceAll("-");
    }

    private OrganizationResponse toResponse(Organization org, OrganizationRole currentUserRole) {
        return new OrganizationResponse(
                org.getId(), org.getName(), org.getSlug(), org.isPersonal(),
                org.getOwner().getId(), currentUserRole.name());
    }

    private MembershipResponse toMembershipResponse(Membership m) {
        return new MembershipResponse(
                m.getId(),
                m.getUser() != null ? m.getUser().getId() : null,
                m.getUser() != null ? m.getUser().getEmail() : m.getInvitedEmail(),
                m.getUser() != null ? m.getUser().getFullName() : null,
                m.getRole(),
                m.getStatus());
    }
}