package com.verbamind.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile({"dev", "prod"})
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${verbamind.mail.from}")
    private String fromAddress;

    @Value("${verbamind.mail.verification-base-url}")
    private String verificationBaseUrl;

    @Value("${verbamind.mail.reset-base-url}")
    private String resetBaseUrl;

    @Value("${verbamind.mail.invite-base-url}")
    private String inviteBaseUrl;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        String link = verificationBaseUrl + "?token=" + token;
        String body = """
                Welcome to Verbamind!

                Please verify your email address by clicking the link below:
                %s

                This link expires in 24 hours. If you didn't create this account, you can ignore this email.
                """.formatted(link);

        send(to, "Verify your Verbamind account", body);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String link = resetBaseUrl + "?token=" + token;
        String body = """
                We received a request to reset your Verbamind password.

                Click the link below to choose a new password:
                %s

                This link expires in 1 hour. If you didn't request this, you can safely ignore this email.
                """.formatted(link);

        send(to, "Reset your Verbamind password", body);
    }

    @Override
    public void sendOrganizationInviteEmail(String to, String organizationName, String token) {
        String link = inviteBaseUrl + "?token=" + token;
        String body = """
                You've been invited to join %s on Verbamind.

                Click the link below to accept the invitation:
                %s

                If you weren't expecting this invite, you can safely ignore this email.
                """.formatted(organizationName, link);

        send(to, "You're invited to join " + organizationName + " on Verbamind", body);
    }

    @Override
    public void sendDocumentReadyEmail(String to, String fileName) {
        String body = """
                Good news — "%s" has finished processing and is ready to use.

                You can now ask questions about it in Verbamind.
                """.formatted(fileName);

        send(to, "\"" + fileName + "\" is ready", body);
    }

    @Override
    public void sendDocumentFailedEmail(String to, String fileName) {
        String body = """
                We ran into a problem processing "%s" and couldn't finish preparing it for search.

                You can try uploading it again, or reach out if this keeps happening.
                """.formatted(fileName);

        send(to, "\"" + fileName + "\" failed to process", body);
    }

    @Override
    public void sendPaymentSuccessEmail(String to, String planName) {
        String body = """
                Thanks for upgrading! Your workspace is now on the %s plan.

                Your new usage limits are active immediately.
                """.formatted(planName);

        send(to, "You're now on the " + planName + " plan", body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent email '{}' to {}", subject, to);
        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage(), e);
        }
    }
}