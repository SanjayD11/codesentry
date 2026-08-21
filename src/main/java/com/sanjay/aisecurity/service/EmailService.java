package com.sanjay.aisecurity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${resend.api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.from-address:sanjayraju5164@gmail.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:CodeSentry Security}")
    private String fromName;

    @PostConstruct
    public void validateConfiguration() {
        if (isResendConfigured()) {
            log.info("[EmailService] Primary email transport: Resend HTTPS REST API (Port 443)");
        } else if (isBrevoConfigured()) {
            log.info("[EmailService] Primary email transport: Brevo HTTPS REST API (Port 443)");
        } else if (isSmtpConfigured()) {
            log.info("[EmailService] Primary email transport: SMTP (Host: {}, From: {} <{}>)", mailHost, fromName, fromAddress);
        } else {
            log.warn("[EmailService] No email transport configured (RESEND_API_KEY / BREVO_API_KEY / SMTP not set). Reset links will be printed to server logs.");
        }
    }

    public boolean isResendConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }

    public boolean isBrevoConfigured() {
        return brevoApiKey != null && !brevoApiKey.isBlank();
    }

    public boolean isSmtpConfigured() {
        return mailHost != null && !mailHost.isBlank()
            && mailUsername != null && !mailUsername.isBlank()
            && mailPassword != null && !mailPassword.isBlank();
    }

    // =========================================================================
    // PASSWORD RESET EMAIL
    // =========================================================================

    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        // ALWAYS log the reset link prominently in server logs
        log.info("================================================================================");
        log.info("🔑 [PASSWORD RESET LINK GENERATED]");
        log.info("🔑 Recipient : {}", to);
        log.info("🔑 Reset URL : {}", resetLink);
        log.info("================================================================================");

        String subject = "Reset Your CodeSentry Password";
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

        dispatchEmail(to, subject, htmlContent);
    }

    // =========================================================================
    // VERIFICATION EMAIL
    // =========================================================================

    @Async
    public void sendVerificationEmail(String to, String verifyLink) {
        log.info("================================================================================");
        log.info("✉️ [EMAIL VERIFICATION LINK GENERATED]");
        log.info("✉️ Recipient  : {}", to);
        log.info("✉️ Verify URL : {}", verifyLink);
        log.info("================================================================================");

        String subject = "Verify your Email — " + fromName;
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

        dispatchEmail(to, subject, htmlContent);
    }

    public void sendTestEmail(String to) {
        String subject = "SMTP & Email Delivery Test — " + fromName;
        String htmlContent = "<h2>Email Delivery Test</h2><p>If you received this email, CodeSentry email dispatch is working perfectly!</p>";
        dispatchEmail(to, subject, htmlContent);
    }

    // =========================================================================
    // MULTI-PROVIDER DISPATCH PIPELINE
    // =========================================================================

    private void dispatchEmail(String to, String subject, String htmlContent) {
        // Strategy 1: Resend HTTP REST API (port 443, never blocked by cloud firewalls)
        if (isResendConfigured()) {
            boolean sent = sendViaResend(to, subject, htmlContent);
            if (sent) return;
        }

        // Strategy 2: Brevo HTTP REST API (port 443)
        if (isBrevoConfigured()) {
            boolean sent = sendViaBrevo(to, subject, htmlContent);
            if (sent) return;
        }

        // Strategy 3: JavaMail SMTP
        if (isSmtpConfigured()) {
            sendViaSmtp(to, subject, htmlContent);
        } else {
            log.info("[EmailService] No active outgoing mail provider. Email link logged above for instant use.");
        }
    }

    private boolean sendViaResend(String to, String subject, String htmlContent) {
        try {
            log.info("[EmailService] Attempting delivery via Resend API to {}", to);
            String from = (fromAddress != null && !fromAddress.isBlank() && !fromAddress.endsWith("@gmail.com"))
                ? (fromName + " <" + fromAddress + ">")
                : "CodeSentry Security <onboarding@resend.dev>";

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", from);
            payload.put("to", Collections.singletonList(to));
            payload.put("subject", subject);
            payload.put("html", htmlContent);

            String json = objectMapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(12))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[EmailService] Successfully sent email to {} via Resend API. Response: {}", to, response.body());
                return true;
            } else {
                log.error("[EmailService] Resend API error (status {}): {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[EmailService] Resend API request failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendViaBrevo(String to, String subject, String htmlContent) {
        try {
            log.info("[EmailService] Attempting delivery via Brevo API to {}", to);
            Map<String, Object> sender = new HashMap<>();
            sender.put("name", fromName);
            sender.put("email", fromAddress);

            Map<String, Object> recipient = new HashMap<>();
            recipient.put("email", to);

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", sender);
            payload.put("to", Collections.singletonList(recipient));
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            String json = objectMapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(12))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[EmailService] Successfully sent email to {} via Brevo API. Response: {}", to, response.body());
                return true;
            } else {
                log.error("[EmailService] Brevo API error (status {}): {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[EmailService] Brevo API request failed: {}", e.getMessage());
            return false;
        }
    }

    private void sendViaSmtp(String to, String subject, String htmlContent) {
        try {
            log.info("[EmailService] Attempting delivery via SMTP (Host: {}) to {}", mailHost, to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Password reset email successfully delivered via SMTP to {}", to);

        } catch (MailAuthenticationException e) {
            log.error("[EmailService] SMTP Authentication Failed: {}", e.getMessage());
        } catch (MailSendException e) {
            log.error("[EmailService] SMTP Connection Failed (Render blocks ports 25/465/587 on standard tiers): {}", e.getMessage());
        } catch (MessagingException e) {
            log.error("[EmailService] Message format error: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[EmailService] SMTP error for recipient {}: {}", to, e.getMessage());
        }
    }
}
