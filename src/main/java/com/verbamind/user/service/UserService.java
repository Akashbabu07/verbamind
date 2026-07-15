package com.verbamind.user.service;

import com.verbamind.auth.entity.User;
import com.verbamind.auth.repository.RefreshTokenRepository;
import com.verbamind.auth.repository.UserRepository;
import com.verbamind.organization.entity.Organization;
import com.verbamind.organization.service.OrganizationService;
import com.verbamind.subscription.dto.SubscriptionResponse;
import com.verbamind.subscription.service.SubscriptionService;
import com.verbamind.usage.dto.UsageResponse;
import com.verbamind.usage.service.UsageService;
import com.verbamind.user.dto.*;
import com.verbamind.user.exception.IncorrectPasswordException;
import com.verbamind.user.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationService organizationService;
    private final SubscriptionService subscriptionService;
    private final UsageService usageService;

    public UserProfileResponse getUserProfile(UUID userId) {
        User user = getUserOrThrow(userId);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);
        user.setFullName(request.fullName());
        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getUserOrThrow(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IncorrectPasswordException("wrong password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    @Transactional
    public void deleteUser(UUID userId, DeleteAccountRequest request) {
        User user = getUserOrThrow(userId);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IncorrectPasswordException("wrong password");
        }
        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);

        refreshTokenRepository.deleteByUserId(user.getId());
    }

    public SubscriptionResponse getSubscriptionSummary(UUID userId) {
        Organization personalWorkspace = organizationService.getPersonalWorkspace(userId);
        return subscriptionService.getSubscription(userId, personalWorkspace.getId());
    }

    public UsageResponse getUsageSummary(UUID userId) {
        Organization personalWorkspace = organizationService.getPersonalWorkspace(userId);
        return usageService.getUsageSummary(userId, personalWorkspace.getId());
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.isEmailVerified(), user.getCreatedAt());
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("user not found "));
    }
}