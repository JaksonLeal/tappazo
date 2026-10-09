package com.tappazo.infrastructure.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tappazo.domain.exception.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Verifies a Google ID Token received from the Flutter client and extracts
 * the user's Google identity. Stateless — creates a new verifier per
 * Spring bean (verifier is thread-safe after construction).
 *
 * Auth flow (Sección 3):
 *   Flutter → Google Login → Google ID Token → Backend (here) → JWT Tappazo
 */
@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(
            @Value("${tappazo.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Verifies the raw Google ID Token string sent from the Flutter app.
     *
     * @param idTokenString raw ID token from Google Sign-In
     * @return {@link GoogleUserInfo} with the verified user data
     * @throws DomainException if the token is invalid, expired, or cannot be verified
     */
    public GoogleUserInfo verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new DomainException("Google ID Token inválido o expirado.");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email    = payload.getEmail();
            String name     = (String) payload.get("name");
            return new GoogleUserInfo(googleId, email, name != null ? name : email);
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Error al verificar Google ID Token: " + e.getMessage());
        }
    }

    /**
     * Immutable value object with the verified Google user data.
     */
    public record GoogleUserInfo(String googleId, String email, String name) {}
}
