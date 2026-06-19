package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.CalendarAccount;
import com.tsp.jimi_api.repositories.CalendarAccountRepository;
import com.tsp.jimi_api.services.crypto.TokenCipher;
import com.tsp.jimi_api.services.oauth.GoogleTokenClient;
import com.tsp.jimi_api.services.oauth.GoogleTokenClient.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Owns linked calendar accounts and their OAuth tokens.
 *
 * <p>All tokens are encrypted via {@link TokenCipher} before they hit the
 * database and decrypted only in memory when a request needs them. Access
 * tokens are refreshed transparently when expired, and revoked at the provider
 * when the user unlinks or deletes their data.
 *
 * <p>This service is provider-aware only through the {@code provider} string +
 * a {@link GoogleTokenClient}; CalDAV/Microsoft will add their own token
 * clients in later PRs.
 */
@Service
public class CalendarAccountService {

    /** Refresh a little before actual expiry to avoid races on slow calls. */
    private static final long EXPIRY_SKEW_SECONDS = 60;

    public static final String GOOGLE = "google";

    private final CalendarAccountRepository repository;
    private final TokenCipher cipher;
    private final GoogleTokenClient googleTokenClient;

    public CalendarAccountService(final CalendarAccountRepository repository,
                                  final TokenCipher cipher,
                                  final GoogleTokenClient googleTokenClient) {
        this.repository = repository;
        this.cipher = cipher;
        this.googleTokenClient = googleTokenClient;
    }

    public boolean isConnected(final String userId, final String provider) {
        return repository.existsByUserIdAndProvider(userId, provider);
    }

    public List<String> connectedProviders(final String userId) {
        return repository.findByUserId(userId).stream()
                .map(CalendarAccount::getProvider)
                .toList();
    }

    /**
     * Creates or updates the user's link for a provider, encrypting the tokens.
     * Preserves an existing refresh token if the provider didn't send a new one.
     */
    @Transactional
    public void link(final String userId, final String provider,
                     final TokenResponse tokens, final String accountEmail) {
        CalendarAccount account = repository.findByUserIdAndProvider(userId, provider)
                .orElseGet(() -> new CalendarAccount(userId, provider));

        account.setAccessTokenEnc(cipher.encrypt(tokens.accessToken()));
        if (tokens.refreshToken() != null) {
            account.setRefreshTokenEnc(cipher.encrypt(tokens.refreshToken()));
        }
        account.setAccessTokenExpiry(tokens.expiry());
        account.setScopes(tokens.scope());
        if (accountEmail != null) {
            account.setAccountEmail(accountEmail);
        }
        repository.save(account);
    }

    /**
     * Returns a valid access token for the user's provider, refreshing it first
     * if it has expired. Throws if the account is missing or cannot be refreshed.
     */
    @Transactional
    public String validAccessToken(final String userId, final String provider) {
        CalendarAccount account = repository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new IllegalStateException("No " + provider + " account linked."));

        if (!isExpired(account)) {
            return cipher.decrypt(account.getAccessTokenEnc());
        }

        String refreshToken = account.getRefreshTokenEnc() == null
                ? null : cipher.decrypt(account.getRefreshTokenEnc());
        if (refreshToken == null) {
            throw new IllegalStateException(
                    "Access token expired and no refresh token available — please reconnect.");
        }

        TokenResponse refreshed = tokenClient(provider).refresh(refreshToken);
        account.setAccessTokenEnc(cipher.encrypt(refreshed.accessToken()));
        account.setAccessTokenExpiry(refreshed.expiry());
        if (refreshed.refreshToken() != null) {
            account.setRefreshTokenEnc(cipher.encrypt(refreshed.refreshToken()));
        }
        repository.save(account);
        return refreshed.accessToken();
    }

    /** Revokes tokens at the provider and removes the link. */
    @Transactional
    public void unlink(final String userId, final String provider) {
        repository.findByUserIdAndProvider(userId, provider).ifPresent(this::revokeAndDelete);
    }

    /** Revokes and removes every linked account for the user. */
    @Transactional
    public int unlinkAll(final String userId) {
        List<CalendarAccount> accounts = repository.findByUserId(userId);
        accounts.forEach(this::revokeAndDelete);
        return accounts.size();
    }

    private void revokeAndDelete(final CalendarAccount account) {
        Optional.ofNullable(account.getRefreshTokenEnc())
                .or(() -> Optional.ofNullable(account.getAccessTokenEnc()))
                .ifPresent(enc -> tokenClient(account.getProvider()).revoke(cipher.decrypt(enc)));
        repository.delete(account);
    }

    private boolean isExpired(final CalendarAccount account) {
        Instant expiry = account.getAccessTokenExpiry();
        return expiry == null || Instant.now().isAfter(expiry.minusSeconds(EXPIRY_SKEW_SECONDS));
    }

    private GoogleTokenClient tokenClient(final String provider) {
        if (GOOGLE.equals(provider)) {
            return googleTokenClient;
        }
        throw new IllegalArgumentException("No token client for provider: " + provider);
    }
}
