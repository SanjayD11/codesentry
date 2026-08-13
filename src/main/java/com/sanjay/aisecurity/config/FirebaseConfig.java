package com.sanjay.aisecurity.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK Configuration.
 *
 * <p>Initializes the Firebase Admin SDK on application startup using the
 * service account credentials file. This configuration is required solely
 * for server-side verification of Firebase ID tokens received from the
 * React frontend during Google/GitHub OAuth login flows.</p>
 *
 * <p>Firebase is used <strong>only</strong> as an identity provider.
 * All application data, users, roles, and JWTs remain under the control
 * of the Spring Boot backend.</p>
 *
 * @author Sanjay
 * @version 1.1.0
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:firebase-service-account.json}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("[Firebase] Already initialized — skipping.");
            return;
        }

        try {
            InputStream serviceAccount;
            String envJson = System.getenv("FIREBASE_CREDENTIALS_JSON");

            if (envJson != null && !envJson.isBlank()) {
                // Load securely from Render environment variable
                serviceAccount = new java.io.ByteArrayInputStream(envJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                log.info("[Firebase] Loading credentials from FIREBASE_CREDENTIALS_JSON environment variable.");
            } else {
                // Fallback to local file for development
                serviceAccount = new ClassPathResource(credentialsPath).getInputStream();
                log.info("[Firebase] Loading credentials from local file: {}", credentialsPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("[Firebase] Admin SDK initialized successfully.");
        } catch (IOException e) {
            log.warn("[Firebase] Service account credentials not found — Firebase OAuth login will be unavailable. "
                   + "Set FIREBASE_CREDENTIALS_JSON in Render or place the file locally.");
        }
    }
}
