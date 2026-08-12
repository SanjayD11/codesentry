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
            InputStream serviceAccount = new ClassPathResource(credentialsPath).getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("[Firebase] Admin SDK initialized successfully.");
        } catch (IOException e) {
            log.warn("[Firebase] Service account file '{}' not found — Firebase OAuth login will be unavailable. "
                   + "Place the file in src/main/resources/ to enable it.", credentialsPath);
        }
    }
}
