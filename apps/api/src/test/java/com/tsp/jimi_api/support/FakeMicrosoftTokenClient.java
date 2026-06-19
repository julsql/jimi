package com.tsp.jimi_api.support;

import com.tsp.jimi_api.services.oauth.MicrosoftTokenClient;

/**
 * Scriptable {@link MicrosoftTokenClient} for tests — no network.
 */
public class FakeMicrosoftTokenClient implements MicrosoftTokenClient {

    public TokenResponse nextResponse;
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
}
