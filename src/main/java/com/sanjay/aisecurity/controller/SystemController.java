package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/system/mail")
@RequiredArgsConstructor
@Tag(name = "System", description = "System configuration and health endpoints")
public class SystemController {

    private final EmailService emailService;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address:}")
    private String fromAddress;

    @Value("${app.mail.from-name:}")
    private String fromName;

    @GetMapping("/health")
    @Operation(summary = "Check SMTP Health", description = "Verifies SMTP configuration, sender settings, and connection.")
    public ResponseEntity<Map<String, Object>> checkMailHealth() {
        Map<String, Object> health = new HashMap<>();
        boolean isConfigured = mailSender instanceof JavaMailSenderImpl;
        health.put("smtpConfigured", isConfigured);

        boolean hasSender = fromAddress != null && !fromAddress.isBlank() && fromName != null && !fromName.isBlank();
        health.put("senderConfigured", hasSender);
        health.put("senderAddress", fromAddress);
        health.put("senderName", fromName);

        boolean connectionSuccessful = false;
        String connectionError = null;

        if (isConfigured) {
            try {
                ((JavaMailSenderImpl) mailSender).testConnection();
                connectionSuccessful = true;
            } catch (MessagingException e) {
                connectionError = e.getMessage();
                log.error("SMTP health check failed: {}", e.getMessage());
            }
        }
        
        health.put("connectionSuccessful", connectionSuccessful);
        if (connectionError != null) {
            health.put("connectionError", connectionError);
        }

        health.put("status", (isConfigured && hasSender && connectionSuccessful) ? "UP" : "DOWN");

        if ("UP".equals(health.get("status"))) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(503).body(health);
        }
    }

    @PostMapping("/test")
    @Operation(summary = "Send Test Email", description = "Sends a test email to verify delivery. Development only.")
    public ResponseEntity<ApiResponse<String>> testEmail(@RequestBody Map<String, String> body) {
        String to = body.get("recipientEmail");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("recipientEmail is required."));
        }
        try {
            emailService.sendTestEmail(to.trim().toLowerCase());
            return ResponseEntity.ok(ApiResponse.success("Test email successfully delivered to SMTP server.", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.internalError("Test email failed: " + e.getMessage()));
        }
    }
}
