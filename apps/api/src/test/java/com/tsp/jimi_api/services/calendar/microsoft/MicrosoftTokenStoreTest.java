package com.tsp.jimi_api.services.calendar.microsoft;

import com.tsp.jimi_api.entities.CalendarAccount;
import com.tsp.jimi_api.services.crypto.TokenCipher;
import com.tsp.jimi_api.services.oauth.MicrosoftTokenClient.TokenResponse;
import com.tsp.jimi_api.support.FakeMicrosoftTokenClient;
import com.tsp.jimi_api.support.InMemoryCalendarAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MicrosoftTokenStoreTest {

    private InMemoryCalendarAccountRepository repo;
    private TokenCipher cipher;
    private FakeMicrosoftTokenClient tokenClient;
    private MicrosoftTokenStore store;

    @BeforeEach
    void setUp() {
        repo = new InMemoryCalendarAccountRepository();
        cipher = new TokenCipher(Base64.getEncoder().encodeToString(new byte[32]));
        tokenClient = new FakeMicrosoftTokenClient();
        store = new MicrosoftTokenStore(repo, cipher, tokenClient);
    }

    private void seed(final String access, final String refresh, final Instant expiry) {
        CalendarAccount a = new CalendarAccount("u1", MicrosoftTokenStore.MICROSOFT);
        a.setAccessTokenEnc(cipher.encrypt(access));
        if (refresh != null) {
            a.setRefreshTokenEnc(cipher.encrypt(refresh));
        }
        a.setAccessTokenExpiry(expiry);
        repo.save(a);
    }

    @Test
    void returnsStoredTokenWhenStillValid() {
        seed("access-1", "refresh-1", Instant.now().plusSeconds(3600));

        assertThat(store.validAccessToken("u1")).isEqualTo("access-1");
        assertThat(tokenClient.refreshCalls).isZero();
    }

    @Test
    void refreshesWhenExpiredAndPersistsTheNewToken() {
        seed("old", "refresh-1", Instant.now().minusSeconds(10));
        tokenClient.nextResponse = new TokenResponse("fresh", null,
                Instant.now().plusSeconds(3600), "Calendars.ReadWrite");

        assertThat(store.validAccessToken("u1")).isEqualTo("fresh");
        assertThat(tokenClient.refreshCalls).isEqualTo(1);
        // Persisted: a second call doesn't refresh again.
        assertThat(store.validAccessToken("u1")).isEqualTo("fresh");
        assertThat(tokenClient.refreshCalls).isEqualTo(1);
    }

    @Test
    void throwsWhenExpiredWithoutRefreshToken() {
        seed("old", null, Instant.now().minusSeconds(10));

        assertThatThrownBy(() -> store.validAccessToken("u1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reconnect");
    }

    @Test
    void throwsWhenNoAccountLinked() {
        assertThatThrownBy(() -> store.validAccessToken("nobody"))
                .isInstanceOf(IllegalStateException.class);
    }
}
