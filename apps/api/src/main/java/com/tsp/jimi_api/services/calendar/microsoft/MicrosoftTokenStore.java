package com.tsp.jimi_api.services.calendar.microsoft;

import com.tsp.jimi_api.entities.CalendarAccount;
import com.tsp.jimi_api.repositories.CalendarAccountRepository;
import com.tsp.jimi_api.services.crypto.TokenCipher;
import com.tsp.jimi_api.services.oauth.MicrosoftTokenClient;
import com.tsp.jimi_api.services.oauth.MicrosoftTokenClient.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Provides a valid Microsoft access token for a user, refreshing it when
 * expired. Kept separate from the Google-specific
 * {@code CalendarAccountService#validAccessToken} so each provider owns its own
 * token lifecycle; storage still goes through the shared {@code calendar_account}
 * table and {@link TokenCipher}.
 */
@Service
public class MicrosoftTokenStore {

    public static final String MICROSOFT = "microsoft";
    private static final long EXPIRY_SKEW_SECONDS = 60;

    private final CalendarAccountRepository repository;
    private final TokenCipher cipher;
    private final MicrosoftTokenClient tokenClient;

    public MicrosoftTokenStore(final CalendarAccountRepository repository,
                               final TokenCipher cipher,
                               final MicrosoftTokenClient tokenClient) {
        this.repository = repository;
        this.cipher = cipher;
        this.tokenClient = tokenClient;
    }

    @Transactional
    public String validAccessToken(final String userId) {
        CalendarAccount account = repository.findByUserIdAndProvider(userId, MICROSOFT)
                .orElseThrow(() -> new IllegalStateException("No Microsoft account linked."));

        if (!isExpired(account)) {
            return cipher.decrypt(account.getAccessTokenEnc());
        }

        String refreshToken = account.getRefreshTokenEnc() == null
                ? null : cipher.decrypt(account.getRefreshTokenEnc());
        if (refreshToken == null) {
            throw new IllegalStateException(
                    "Access token expired and no refresh token available — please reconnect.");
        }

        TokenResponse refreshed = tokenClient.refresh(refreshToken);
        account.setAccessTokenEnc(cipher.encrypt(refreshed.accessToken()));
        account.setAccessTokenExpiry(refreshed.expiry());
        if (refreshed.refreshToken() != null) {
            account.setRefreshTokenEnc(cipher.encrypt(refreshed.refreshToken()));
        }
        repository.save(account);
        return refreshed.accessToken();
    }

    private boolean isExpired(final CalendarAccount account) {
        Instant expiry = account.getAccessTokenExpiry();
        return expiry == null || Instant.now().isAfter(expiry.minusSeconds(EXPIRY_SKEW_SECONDS));
    }
}
