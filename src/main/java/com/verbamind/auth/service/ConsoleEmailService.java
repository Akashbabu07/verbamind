package com.verbamind.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void sendVerificationEmail(String to, String token) {
        log.info("[EMAIL] Verification link for {}: /verify-email?token={}", to, token);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.info("[EMAIL] Password reset link for {}: /reset-password?token={}", to, token);
    }
}