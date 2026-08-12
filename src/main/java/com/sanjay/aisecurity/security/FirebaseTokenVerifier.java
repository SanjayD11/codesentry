package com.sanjay.aisecurity.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Firebase ID Token Verifier.
 *
 * <p>Verifies Firebase ID tokens received from the React frontend after
 * a Google or GitHub OAuth popup login. Extracts the user's email, display
 * name, profile picture, and the sign-in provider from the verified token.</p>
 *
 * <p>This service deliberately has no dependency on any application-layer
 * code — it is a pure security utility that bridges Firebase Authentication
 * with the Spring Boot backend.</p>
 *
 * @author Sanjay
 * @version 1.1.0
 */
@Slf4j
@Service
public class FirebaseTokenVerifier {

    /**
     * Immutable record holding the verified identity extracted from a Firebase token.
     */
    public record FirebaseUserInfo(
            String uid,
            String email,
            String name,
            String picture,
            String provider
    ) {}

    /**
     * Verifies the given Firebase ID token and extracts user identity.
     *
     * @param idToken the raw Firebase ID token string from the frontend
     * @return a {@link FirebaseUserInfo} with the verified user's details
     * @throws IllegalStateException     if Firebase Admin SDK is not initialized
     * @throws IllegalArgumentException  if the token is invalid, expired, or revoked
     */
    public FirebaseUserInfo verifyToken(String idToken) {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException(
                    "Firebase Admin SDK is not initialized. "
                  + "Place firebase-service-account.json in src/main/resources/.");
        }

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            String uid      = decodedToken.getUid();
            String email     = decodedToken.getEmail();
            String name      = decodedToken.getName();
            String picture   = decodedToken.getPicture();
            String provider  = extractProvider(decodedToken);

            log.info("[Firebase] Token verified for uid={}, email={}, provider={}",
                    uid, email, provider);

            return new FirebaseUserInfo(uid, email, name, picture, provider);

        } catch (FirebaseAuthException e) {
            log.error("[Firebase] Token verification failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired Firebase token.", e);
        }
    }

    /**
     * Extracts the sign-in provider (google.com, github.com, password) from
     * the Firebase token's sign_in_provider claim.
     */
    private String extractProvider(FirebaseToken token) {
        Object firebase = token.getClaims().get("firebase");
        if (firebase instanceof java.util.Map<?, ?> firebaseMap) {
            Object signInProvider = firebaseMap.get("sign_in_provider");
            if (signInProvider != null) {
                return signInProvider.toString();
            }
        }
        return "unknown";
    }
}
