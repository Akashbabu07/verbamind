package com.verbamind.admin.service;

import com.verbamind.admin.dto.*;
import com.verbamind.auth.entity.User;
import com.verbamind.auth.repository.RefreshTokenRepository;
import com.verbamind.auth.repository.UserRepository;
import com.verbamind.user.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminUserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public AdminUserPageResponse listUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        var items = page.getContent().stream().map(this::toResponse).toList();
        return new AdminUserPageResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public AdminUserResponse getUser(UUID userId) {
        return toResponse(getUserOrThrow(userId));
    }

    @Transactional
    public AdminUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        User user = getUserOrThrow(userId);
        user.setEnabled(request.enabled());
        userRepository.save(user);

        if (!request.enabled()) {
            refreshTokenRepository.deleteByUserId(user.getId()); // force logout everywhere on disable
        }
        return toResponse(user);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.isEmailVerified(), user.isEnabled(), user.isDeleted(), user.getCreatedAt());
    }
}