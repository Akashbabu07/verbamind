package com.verbamind.auth.service;

import com.verbamind.auth.dto.*;
import com.verbamind.auth.entity.RefreshToken;
import com.verbamind.auth.entity.User;
import com.verbamind.auth.exception.EmailAlreadyExistsException;
import com.verbamind.auth.exception.InvalidCredentialsException;
import com.verbamind.auth.exception.InvalidTokenException;
import com.verbamind.auth.repository.RefreshTokenRepository;
import com.verbamind.auth.repository.UserRepository;
import com.verbamind.organization.service.OrganizationService;
import com.verbamind.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final OrganizationService organizationService;
    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       EmailService emailService,
                       OrganizationService organizationService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.organizationService = organizationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setVerificationToken(UUID.randomUUID().toString());

        try {
            // existsByEmail() above and this save() aren't atomic, so two concurrent
            // registrations with the same email can both pass the check. The unique
            // constraint on users.email is the real guard; translate its violation into
            // the same clean 409 instead of letting it surface as a generic 500.
            userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(request.email());
        }

        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
        organizationService.createPersonalWorkspace(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            // Covers bad credentials as well as DisabledException/LockedException thrown
            // for disabled or deleted accounts. Deliberately don't distinguish the reason
            // in the response, to avoid leaking account status to an unauthenticated caller.
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken existing = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return issueTokens(existing.getUser());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            user.setResetToken(UUID.randomUUID().toString());
            user.setResetTokenExpiresAt(Instant.now().plusSeconds(3600)); // 1 hour
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getResetToken());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Invalid or expired reset token");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);

        refreshTokenRepository.deleteByUserId(user.getId()); // force re-login everywhere
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByVerificationToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtService.generateRefreshTokenValue());
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()));
        refreshTokenRepository.save(refreshToken);

        UserSummary summary = new UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.isEmailVerified());
        return AuthResponse.of(accessToken, refreshToken.getToken(), summary);
    }
}