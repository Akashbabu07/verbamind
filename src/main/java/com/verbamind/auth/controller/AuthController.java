package com.verbamind.auth.controller;

import com.verbamind.auth.dto.*;
import com.verbamind.auth.service.AuthService;
import com.verbamind.common.dto.ApiResponse;
import com.verbamind.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    public AuthController(AuthService authService, RateLimitService rateLimitService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                                HttpServletRequest httpRequest) {
        // 10 signups per IP per hour — generous for real users, blocks mass account creation
        rateLimitService.checkRateLimit("register", clientIp(httpRequest), 10, Duration.ofHours(1));
        return ResponseEntity.ok(ApiResponse.success(authService.register(request), "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                             HttpServletRequest httpRequest) {
        // 5 attempts per IP per 15 min — the classic brute-force guard
        rateLimitService.checkRateLimit("login", clientIp(httpRequest), 5, Duration.ofMinutes(15));
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                              HttpServletRequest httpRequest) {
        // 3 per IP per hour — this endpoint sends an email, so it's the easiest one to spam
        rateLimitService.checkRateLimit("forgot-password", clientIp(httpRequest), 3, Duration.ofHours(1));
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "If that email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successful"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified"));
    }

    /**
     * Prefers X-Forwarded-For (set by reverse proxies/load balancers in real
     * deployments) and falls back to the raw remote address for local/dev use.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}