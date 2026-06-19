package com.tsp.jimi_api.services.oauth;

import java.time.Instant;

/**
 * Talks to Microsoft's identity platform token endpoint. Behind an interface so
 * the token-refresh logic is unit-testable without any network call. Mirrors
 * {@link GoogleTokenClient}.
 */
public interface MicrosoftTokenClient {

    record TokenResponse(String accessToken, String refreshToken, Instant expiry, String scope) {
    }

    /** Exchanges an authorization code (+ PKCE verifier) for tokens. */
    TokenResponse exchangeCode(String code, String codeVerifier);

    /** Uses a refresh token to obtain a fresh access token. */
    TokenResponse refresh(String refreshToken);
}
