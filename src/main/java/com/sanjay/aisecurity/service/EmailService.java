package com.sanjay.aisecurity.service;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final Environment environment;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.from-address:}")
    private String fromAddress;

    @Value("${app.mail.from-name:}")
    private String fromName;

    @PostConstruct
    public void validateConfiguration() {
        if (!isSmtpConfigured() && !isDevProfile()) {
            log.warn("SMTP is not configured in production. Emails will fail.");
        }
        
        if (isSmtpConfigured()) {
            if (fromAddress == null || fromAddress.isBlank()) {
                throw new IllegalStateException("CONFIGURATION ERROR: 'app.mail.from-address' is not configured. A verified sender email is required.");
            }
            if (fromName == null || fromName.isBlank()) {
                throw new IllegalStateException("CONFIGURATION ERROR: 'app.mail.from-name' is not configured. A sender name is required.");
            }
            log.info("Email service initialized. Sending emails FROM: {} <{}>", fromName, fromAddress);
        }
    }

    private boolean isSmtpConfigured() {
        return mailHost != null && !mailHost.isBlank()
            && mailUsername != null && !mailUsername.isBlank()
            && mailPassword != null && !mailPassword.isBlank();
    }

    private boolean isDevProfile() {
        return !Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        if (isDevProfile()) {
            log.info("==========================================================");
            log.info("[DEV] PASSWORD RESET LINK GENERATED");
            log.info("[DEV] Recipient : {}", to);
            log.info("[DEV] Reset URL : {}", resetLink);
            log.info("==========================================================");
        }

        if (!isSmtpConfigured()) {
            log.warn("[DEV] SMTP is NOT configured (mail.host / mail.username / mail.password are empty).");
            return;
        }

        try {
            log.info("Attempting to send Password Reset Email - Sender: {}, Recipient: {}", fromAddress, to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject("Reset Your CodeSentry Password");

            String htmlContent =
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f9fafb; padding: 40px; border-radius: 12px; border: 1px solid #e5e7eb;'>"
                + "<div style='text-align: center; margin-bottom: 32px;'>"
                + "<h1 style='color: #111827; font-size: 22px; margin: 0;'>" + fromName + "</h1>"
                + "</div>"
                + "<p style='color: #374151;'>Hello,</p>"
                + "<p style='color: #374151;'>We received a request to reset your CodeSentry password.</p>"
                + "<div style='text-align: center; margin: 32px 0;'>"
                + "<a href='" + resetLink + "' style='background-color: #0058be; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; font-size: 15px;'>Reset Password</a>"
                + "</div>"
                + "<p style='color: #6b7280; font-size: 13px;'>This link expires in 15 minutes.</p>"
                + "<p style='color: #6b7280; font-size: 13px;'>If the button doesn't work:<br><a href='" + resetLink + "'>" + resetLink + "</a></p>"
                + "<p style='color: #6b7280; font-size: 13px;'>If you didn't request this, you can safely ignore this email.</p>"
                + "<hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'/>"
                + "<p style='color: #9ca3af; font-size: 12px; text-align: center;'>— CodeSentry Security Team</p>"
                + "</div>";

            helper.setText(htmlContent, true);
            
            log.info("Sending email...");
            mailSender.send(message);
            log.info("Password reset email successfully sent to {}", to);

        } catch (MailAuthenticationException e) {
            log.error("SMTP Authentication Failed. Invalid App Password or credentials: {}", e.getMessage());
            throw new RuntimeException("Email delivery failed: SMTP Authentication Failed", e);
        } catch (MailSendException e) {
            log.error("Connection Timeout or SMTP failure: {}", e.getMessage());
            throw new RuntimeException("Email delivery failed: Connection Timeout", e);
        } catch (MessagingException e) {
            log.error("Message formulation error: {}", e.getMessage());
            throw new RuntimeException("Email delivery failed: Message Error", e);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}. Exception: {}", to, e.getClass().getName(), e);
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    public void sendVerificationEmail(String to, String verifyLink) {
        if (isDevProfile()) {
            log.info("==========================================================");
            log.info("[DEV] EMAIL VERIFICATION LINK GENERATED");
            log.info("[DEV] Recipient  : {}", to);
            log.info("[DEV] Verify URL : {}", verifyLink);
            log.info("==========================================================");
        }

        if (!isSmtpConfigured()) {
            log.warn("[DEV] SMTP not configured. Verification email NOT sent. Use the link logged above.");
            return;
        }

        try {
            log.info("Attempting to send Verification Email - Sender: {}, Recipient: {}, Subject: Verify your Email", fromAddress, to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject("Verify your Email — " + fromName);

            String htmlContent =
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;"
                + " background-color: #f9fafb; padding: 40px; border-radius: 12px;"
                + " border: 1px solid #e5e7eb;'>"
                + "<div style='text-align: center; margin-bottom: 32px;'>"
                + "<h1 style='color: #111827; font-size: 22px; margin: 0;'>" + fromName + "</h1>"
                + "<p style='color: #6b7280; margin: 4px 0 0; font-size: 13px;'>AI-Powered Source Code Security Platform</p>"
                + "</div>"
                + "<h2 style='color: #111827; font-size: 18px;'>Email Verification</h2>"
                + "<p style='color: #374151;'>Hello,</p>"
                + "<p style='color: #374151;'>Welcome to CodeSentry! Please verify your email address"
                + " to activate your account:</p>"
                + "<div style='text-align: center; margin: 32px 0;'>"
                + "<a href='" + verifyLink + "' style='background-color: #0058be; color: white;"
                + " padding: 14px 28px; text-decoration: none; border-radius: 8px;"
                + " font-weight: bold; display: inline-block; font-size: 15px;'>Verify Email</a>"
                + "</div>"
                + "<p style='color: #6b7280; font-size: 13px;'>This link will expire in <strong>24 hours</strong>.</p>"
                + "<hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'/>"
                + "<p style='color: #9ca3af; font-size: 12px; text-align: center;'>"
                + "© " + fromName + " Security Platform</p>"
                + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email Verification successfully sent to {}", to);

        } catch (MailAuthenticationException e) {
            log.error("SMTP Authentication Failed. Invalid App Password or credentials: {}", e.getMessage());
        } catch (MailSendException e) {
            log.error("Connection Timeout or SMTP failure: {}", e.getMessage());
        } catch (MessagingException e) {
            log.error("Message formulation error: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send verification email to {}. Exception: {}", to, e.getClass().getName(), e);
        }
    }

    public void sendTestEmail(String to) {
        log.info("Attempting to send Test Email - Sender: {}, Recipient: {}, Subject: SMTP Configuration Test", fromAddress, to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject("SMTP Configuration Test — " + fromName);

            String htmlContent = "<h2>SMTP Configuration Test</h2><p>If you are reading this, Gmail SMTP is working perfectly with the verified sender.</p>";
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Successfully delivered Test email to Gmail SMTP server for recipient: {}. MessageID: {}", to, message.getMessageID());
        } catch (Exception e) {
            log.error("SMTP TEST FAILED. Full exception logged below for debugging.", e);
            throw new RuntimeException("Test email failed: " + e.getMessage(), e);
        }
    }
}
