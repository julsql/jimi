package com.tsp.jimi_api.services.oauth;

import com.tsp.jimi_api.configurations.GoogleOAuthProperties;
import com.tsp.jimi_api.services.CalendarAccountService;
import com.tsp.jimi_api.services.calendar.google.GoogleApiClient;
import com.tsp.jimi_api.services.oauth.GoogleTokenClient.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Drives the Google OAuth 2.0 authorization-code flow (with PKCE).
 *
 * <ol>
 *   <li>{@link #authorizationUrl} builds the consent URL the app opens.</li>
 *   <li>Google redirects back to our callback; {@link #handleCallback} exchanges
 *       the code for tokens, stores them encrypted, and returns the deep link to
 *       bounce the user back into the app.</li>
 * </ol>
 *
 * <p>We request {@code access_type=offline} + {@code prompt=consent} so Google
 * returns a refresh token, and only the {@code calendar.events} scope.
 */
@Service
public class GoogleOAuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthService.class);

    private final GoogleOAuthProperties props;
    private final OAuthStateCodec stateCodec;
    private final GoogleTokenClient tokenClient;
    private final CalendarAccountService accounts;
    private final GoogleApiClient api;

    public GoogleOAuthService(final GoogleOAuthProperties props,
                              final OAuthStateCodec stateCodec,
                              final GoogleTokenClient tokenClient,
                              final CalendarAccountService accounts,
                              final GoogleApiClient api) {
        this.props = props;
        this.stateCodec = stateCodec;
        this.tokenClient = tokenClient;
        this.accounts = accounts;
        this.api = api;
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    public String authorizationUrl(final String userId) {
        OAuthStateCodec.IssuedState issued = stateCodec.issue(userId);
        return UriComponentsBuilder.fromHttpUrl(props.getAuthUri())
                .queryParam("client_id", props.getClientId())
                .queryParam("redirect_uri", props.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", props.getScope())
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", issued.state())
                .queryParam("code_challenge", issued.codeChallenge())
                .queryParam("code_challenge_method", "S256")
                .encode()
                .toUriString();
    }

    /**
     * Exchanges the authorization code for tokens, links the account, and
     * returns the app deep link to redirect to.
     */
    public String handleCallback(final String code, final String state) {
        OAuthStateCodec.StatePayload payload = stateCodec.verify(state);
        TokenResponse tokens = tokenClient.exchangeCode(code, payload.codeVerifier());

        accounts.link(payload.userId(), CalendarAccountService.GOOGLE, tokens, null);

        // Best-effort: record which Google account was linked (for display).
        String email = null;
        try {
            email = api.primaryCalendarEmail(payload.userId());
            accounts.link(payload.userId(), CalendarAccountService.GOOGLE, tokens, email);
        } catch (Exception e) {
            LOGGER.warn("[oauth] could not fetch primary calendar email: {}", e.getMessage());
        }

        return appReturn("connected");
    }

    public String appReturn(final String status) {
        return UriComponentsBuilder.fromUriString(props.getAppReturnUrl())
                .queryParam("status", status)
                .build().toUriString();
    }
}
