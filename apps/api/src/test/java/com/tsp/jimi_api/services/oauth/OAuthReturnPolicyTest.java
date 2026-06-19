package com.tsp.jimi_api.services.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthReturnPolicyTest {

    private final OAuthReturnPolicy policy = new OAuthReturnPolicy();
    private static final String FALLBACK = "jimi://connected";

    @Test
    void honoursAWebReturnUrl() {
        String result = policy.resolve("https://jimi.julsql.fr/connected", FALLBACK, "connected");
        assertThat(result).isEqualTo("https://jimi.julsql.fr/connected?status=connected");
    }

    @Test
    void honoursANativeCustomSchemeReturnUrl() {
        String result = policy.resolve("jimi://connected", FALLBACK, "connected");
        assertThat(result).isEqualTo("jimi://connected?status=connected");
    }

    @Test
    void fallsBackWhenReturnUrlIsMissing() {
        assertThat(policy.resolve(null, FALLBACK, "error"))
                .isEqualTo("jimi://connected?status=error");
        assertThat(policy.resolve("  ", FALLBACK, "connected"))
                .isEqualTo("jimi://connected?status=connected");
    }

    @Test
    void rejectsAMalformedOrDangerousReturnUrl() {
        // not a scheme://… target → fall back rather than redirect anywhere odd
        assertThat(policy.resolve("not-a-url", FALLBACK, "connected"))
                .isEqualTo("jimi://connected?status=connected");
        assertThat(policy.resolve("javascript:alert(1)", FALLBACK, "connected"))
                .isEqualTo("jimi://connected?status=connected");
    }
}
