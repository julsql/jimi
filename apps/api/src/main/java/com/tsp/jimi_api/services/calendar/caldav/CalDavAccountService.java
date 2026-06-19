package com.tsp.jimi_api.services.calendar.caldav;

import com.tsp.jimi_api.entities.CalendarAccount;
import com.tsp.jimi_api.repositories.CalendarAccountRepository;
import com.tsp.jimi_api.services.crypto.TokenCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores and reads CalDAV credentials in the shared {@code calendar_account}
 * table (provider {@code "caldav"}).
 *
 * <p>Unlike Google, CalDAV has no bearer token to refresh: the whole
 * {@link CalDavCredentials} blob is encrypted with {@link TokenCipher} into the
 * {@code access_token_enc} column. {@code refresh_token_enc} stays null and the
 * collection URL is mirrored into {@code account_email} purely for display in
 * {@code GET /connections}.
 */
@Service
public class CalDavAccountService {

    public static final String CALDAV = "caldav";

    private final CalendarAccountRepository repository;
    private final TokenCipher cipher;

    public CalDavAccountService(final CalendarAccountRepository repository, final TokenCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    public boolean isConnected(final String userId) {
        return repository.existsByUserIdAndProvider(userId, CALDAV);
    }

    /** Creates or updates the user's CalDAV link, encrypting the credentials. */
    @Transactional
    public void link(final String userId, final CalDavCredentials creds) {
        CalendarAccount account = repository.findByUserIdAndProvider(userId, CALDAV)
                .orElseGet(() -> new CalendarAccount(userId, CALDAV));
        account.setAccessTokenEnc(cipher.encrypt(creds.toJson()));
        account.setRefreshTokenEnc(null);
        account.setAccountEmail(creds.server());
        account.setScopes(null);
        repository.save(account);
    }

    /** Decrypts and returns the stored credentials, or throws if not linked. */
    public CalDavCredentials credentials(final String userId) {
        CalendarAccount account = repository.findByUserIdAndProvider(userId, CALDAV)
                .orElseThrow(() -> new IllegalStateException("No CalDAV account linked."));
        return CalDavCredentials.fromJson(cipher.decrypt(account.getAccessTokenEnc()));
    }

    /** Removes the link. There is no token to revoke for CalDAV. */
    @Transactional
    public void unlink(final String userId) {
        repository.findByUserIdAndProvider(userId, CALDAV).ifPresent(repository::delete);
    }
}
