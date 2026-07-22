package com.verbamind.auth.service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendPasswordResetEmail(String to, String token);
    void sendOrganizationInviteEmail(String to, String organizationName, String token);
    void sendDocumentReadyEmail(String to, String fileName);
    void sendDocumentFailedEmail(String to, String fileName);
    void sendPaymentSuccessEmail(String to, String planName);
}