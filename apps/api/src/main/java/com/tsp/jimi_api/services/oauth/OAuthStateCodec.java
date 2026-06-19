package com.tsp.jimi_api.services.oauth;

import com.tsp.jimi_api.services.crypto.TokenCipher;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Encodes/decodes the OAuth {@code state} parameter and drives PKCE — without
 * any server-side session store.
 *
 * <p>The state is an <em>encrypted</em> blob (via {@link TokenCipher}) carrying
 * the {@code userId}, the PKCE {@code code_verifier} and an issue timestamp.
 * Because it is authenticated-encrypted, a tampered or forged state fails to
 * decrypt — so it doubles as CSRF protection — and because the verifier travels
 * inside it, the callback is fully stateless yet still does PKCE (S256).
 */
@Component
public class OAuthStateCodec {

    /** A state older than this is rejected (defends against replay). */
    private static final long MAX_AGE_SECONDS = 600;

    private final TokenCipher cipher;
    private final SecureRandom random = new SecureRandom();

    public OAuthStateCodec(final TokenCipher cipher) {
        this.cipher = cipher;
    }

    /** What to embed in the authorization URL. */
    public record IssuedState(String state, String codeChallenge) {
    }

    /** What we recover on the callback. */
    public record StatePayload(String userId, String codeVerifier) {
    }

    public IssuedState issue(final String userId) {
        String codeVerifier = randomUrlSafe();
        JSONObject payload = new JSONObject()
                .put("uid", userId)
                .put("cv", codeVerifier)
                .put("ts", Instant.now().getEpochSecond());
        String state = cipher.encrypt(payload.toString());
        return new IssuedState(state, challengeFor(codeVerifier));
    }

    public StatePayload verify(final String state) {
        JSONObject payload = new JSONObject(cipher.decrypt(state));
        long ts = payload.getLong("ts");
        if (Instant.now().getEpochSecond() - ts > MAX_AGE_SECONDS) {
            throw new IllegalStateException("OAuth state expired; please retry the connection.");
        }
        return new StatePayload(payload.getString("uid"), payload.getString("cv"));
    }

    private String randomUrlSafe() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String challengeFor(final String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute PKCE challenge", e);
        }
    }
}
