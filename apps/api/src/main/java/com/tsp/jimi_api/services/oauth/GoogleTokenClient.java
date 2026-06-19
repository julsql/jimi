package com.tsp.jimi_api.services.oauth;

import java.time.Instant;

/**
 * Talks to Google's OAuth token endpoint. Behind an interface so the
 * token-refresh logic in {@code CalendarAccountService} can be unit-tested
 * without any network call.
 */
public interface GoogleTokenClient {

    /**
     * Tokens returned by Google. {@code refreshToken} is only present on the
     * first authorization (when {@code access_type=offline&prompt=consent}).
     */
    record TokenResponse(String accessToken, String refreshToken, Instant expiry, String scope) {
    }

    /** Exchanges an authorization code (+ PKCE verifier) for tokens. */
    TokenResponse exchangeCode(String code, String codeVerifier);

    /** Uses a refresh token to obtain a fresh access token. */
    TokenResponse refresh(String refreshToken);

    /** Revokes a token at the provider so it can no longer be used. Best-effort. */
    void revoke(String token);
}
