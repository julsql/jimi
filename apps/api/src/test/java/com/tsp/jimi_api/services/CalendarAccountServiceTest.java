package com.tsp.jimi_api.services;

import com.tsp.jimi_api.services.crypto.TokenCipher;
import com.tsp.jimi_api.services.oauth.GoogleTokenClient.TokenResponse;
import com.tsp.jimi_api.support.FakeGoogleTokenClient;
import com.tsp.jimi_api.support.InMemoryCalendarAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarAccountServiceTest {

    private static final String GOOGLE = CalendarAccountService.GOOGLE;

    private InMemoryCalendarAccountRepository repo;
    private FakeGoogleTokenClient tokenClient;
    private CalendarAccountService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryCalendarAccountRepository();
        tokenClient = new FakeGoogleTokenClient();
        TokenCipher cipher = new TokenCipher(Base64.getEncoder().encodeToString(new byte[32]));
        service = new CalendarAccountService(repo, cipher, tokenClient);
    }

    private TokenResponse tokens(final String access, final String refresh, final Instant expiry) {
        return new TokenResponse(access, refresh, expiry, "calendar.events");
    }

    @Test
    void linkThenReturnValidTokenWithoutRefreshing() {
        service.link("u1", GOOGLE, tokens("access-1", "refresh-1", Instant.now().plusSeconds(3600)), "me@gmail.com");

        assertThat(service.isConnected("u1", GOOGLE)).isTrue();
        assertThat(service.validAccessToken("u1", GOOGLE)).isEqualTo("access-1");
        assertThat(tokenClient.refreshCalls).isZero();
    }

    @Test
    void refreshesAnExpiredAccessToken() {
        service.link("u1", GOOGLE, tokens("old", "refresh-1", Instant.now().minusSeconds(10)), null);
        tokenClient.nextResponse = tokens("fresh", null, Instant.now().plusSeconds(3600));

        String token = service.validAccessToken("u1", GOOGLE);

        assertThat(token).isEqualTo("fresh");
        assertThat(tokenClient.refreshCalls).isEqualTo(1);
        // The refreshed token is persisted (a second call doesn't refresh again).
        assertThat(service.validAccessToken("u1", GOOGLE)).isEqualTo("fresh");
        assertThat(tokenClient.refreshCalls).isEqualTo(1);
    }

    @Test
    void expiredWithNoRefreshToken_asksUserToReconnect() {
        service.link("u1", GOOGLE, tokens("old", null, Instant.now().minusSeconds(10)), null);

        assertThatThrownBy(() -> service.validAccessToken("u1", GOOGLE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reconnect");
    }

    @Test
    void unlinkRevokesAtProviderAndRemovesTheAccount() {
        service.link("u1", GOOGLE, tokens("access-1", "refresh-1", Instant.now().plusSeconds(3600)), null);

        service.unlink("u1", GOOGLE);

        assertThat(service.isConnected("u1", GOOGLE)).isFalse();
        assertThat(tokenClient.revoked).containsExactly("refresh-1");
    }

    @Test
    void linkPreservesAnExistingRefreshTokenWhenProviderOmitsIt() {
        service.link("u1", GOOGLE, tokens("a1", "refresh-1", Instant.now().plusSeconds(3600)), null);
        // Re-link without a new refresh token (e.g. a later consent without offline access).
        service.link("u1", GOOGLE, tokens("a2", null, Instant.now().minusSeconds(10)), null);
        tokenClient.nextResponse = tokens("a3", null, Instant.now().plusSeconds(3600));

        // Refresh must still work using the preserved refresh token.
        assertThat(service.validAccessToken("u1", GOOGLE)).isEqualTo("a3");
    }
}
