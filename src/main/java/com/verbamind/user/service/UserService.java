package com.verbamind.user.service;

import com.verbamind.auth.repository.RefreshTokenRepository;
import com.verbamind.auth.repository.UserRepository;
import com.verbamind.user.dto.*;
import com.verbamind.user.exception.IncorrectPasswordException;
import com.verbamind.user.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.verbamind.auth.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenRepository refreshTokenRepository;

    public UserProfileResponse getUserProfile(UUID userId) {
        User user= getUserOrThrow(userId);
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
    /**
     * PLACEHOLDER — replace with a real call into SubscriptionService once
     * Step 8 (Subscription) is built. For now every user is reported as being
     * on the Free plan with the V1 default limits.
     */
    public SubscriptionSummaryResponse getSubscriptionSummary(UUID userId) {
        getUserOrThrow(userId);
        return new SubscriptionSummaryResponse(
                "FREE", "ACTIVE", 1_073_741_824L /* 1 GB */, 20, 200);
    }

    /**
     * PLACEHOLDER — replace with a real call into UsageService once Step 10
     * (Usage Tracking) is built. Returns zeros until then.
     */
    public UsageSummaryResponse getUsageSummary(UUID userId) {
        getUserOrThrow(userId);
        return new UsageSummaryResponse(0, 0, 0, 0, 0);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return  new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.isEmailVerified(), user.getCreatedAt());
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(()->new UserNotFoundException("user not found "));
    }
}
