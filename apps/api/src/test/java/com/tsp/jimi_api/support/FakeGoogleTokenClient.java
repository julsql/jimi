package com.tsp.jimi_api.support;

import com.tsp.jimi_api.services.oauth.GoogleTokenClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Scriptable {@link GoogleTokenClient} for tests — no network. Records revoked
 * tokens and returns a queued response on refresh/exchange.
 */
public class FakeGoogleTokenClient implements GoogleTokenClient {

    public TokenResponse nextResponse;
    public final List<String> revoked = new ArrayList<>();
    public int refreshCalls = 0;

    @Override
    public TokenResponse exchangeCode(final String code, final String codeVerifier) {
        return nextResponse;
    }

    @Override
    public TokenResponse refresh(final String refreshToken) {
        refreshCalls++;
        return nextResponse;
    }

    @Override
    public void revoke(final String token) {
        revoked.add(token);
    }
}
