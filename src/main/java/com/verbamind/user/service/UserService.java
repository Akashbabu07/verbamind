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

/***
 * this class marked as service means we are going to use this later  it contain
 * some Business logic
 * spring create and manage the life cycle of object
 * ***/
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationService organizationService;
    private final SubscriptionService subscriptionService;
    private final UsageService usageService;

    /**
     * this method helps to get user register in application  by using unique id
     * By using helper method getUserOrThrow
     * **/
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = getUserOrThrow(userId);
        return toProfileResponse(user);
    }


    /**
     * this method help us to update user profile and this method marked as transactional because it's working
     * with database so it would be safe if this method perform operation either it completes or if some issue happen
     *  it will roll back
     **/
    @Transactional
    public UserProfileResponse updateUserProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);
        user.setFullName(request.fullName());
        userRepository.save(user);
        return toProfileResponse(user);
    }


    /**
     *  this marked as transactional either changed succeed or it will roll Back to original
     *  this method change  password of existing user
     * **/
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


    /**
     * this method marked as Transaction either changed succeed or it will roll Back to original
     * this method delete the user from our database
     * it will match the password for confirmation is it matched user can be deleted .But we are not completely
     * delete user from database we setDeleted as true so database should be consistent this is called as soft
     * delete and set enabled as false this is restricted user to logged in again
     *  and in last we are going to delete refresh token belong to this user
     * **/
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
     * this method is responsible to get information about the user like his summary
     * **/
    public SubscriptionResponse getSubscriptionSummary(UUID userId) {
        Organization personalWorkspace = organizationService.getPersonalWorkspace(userId);
        return subscriptionService.getSubscription(userId, personalWorkspace.getId());
    }


    /**
     * this method return the Summary of usage of application
     * **/
    public UsageResponse getUsageSummary(UUID userId) {
        Organization personalWorkspace = organizationService.getPersonalWorkspace(userId);
        return usageService.getUsageSummary(userId, personalWorkspace.getId());
    }

    /**
     *this is helper method which mapped the user information
     */
    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.isEmailVerified(), user.getCreatedAt());
    }


    /**
     *this is helper function  it find the user from database through repository or return error if not found
     */
    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("user not found "));
    }
}