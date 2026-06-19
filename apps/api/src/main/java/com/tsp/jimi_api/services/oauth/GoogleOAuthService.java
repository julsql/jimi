package com.tsp.jimi_api.services.oauth;

import com.tsp.jimi_api.configurations.GoogleOAuthProperties;
import com.tsp.jimi_api.services.CalendarAccountService;
import com.tsp.jimi_api.services.oauth.GoogleTokenClient.TokenResponse;
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

    private final GoogleOAuthProperties props;
    private final OAuthStateCodec stateCodec;
    private final GoogleTokenClient tokenClient;
    private final CalendarAccountService accounts;
    private final OAuthReturnPolicy returnPolicy;

    public GoogleOAuthService(final GoogleOAuthProperties props,
                              final OAuthStateCodec stateCodec,
                              final GoogleTokenClient tokenClient,
                              final CalendarAccountService accounts,
                              final OAuthReturnPolicy returnPolicy) {
        this.props = props;
        this.stateCodec = stateCodec;
        this.tokenClient = tokenClient;
        this.accounts = accounts;
        this.returnPolicy = returnPolicy;
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    public String authorizationUrl(final String userId, final String returnUrl) {
        OAuthStateCodec.IssuedState issued = stateCodec.issue(userId, returnUrl);
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

        // We intentionally request only the minimal `calendar.events` scope,
        // which does NOT grant access to the Calendars.Get metadata endpoint —
        // so we don't look up the account email here (it would 403). The event
        // CRUD endpoints are fully covered by this scope.
        accounts.link(payload.userId(), CalendarAccountService.GOOGLE, tokens, null);

        return returnPolicy.resolve(payload.returnUrl(), props.getAppReturnUrl(), "connected");
    }

    /** Error/fallback return when there is no valid state (e.g. user denied). */
    public String appReturn(final String status) {
        return returnPolicy.resolve(null, props.getAppReturnUrl(), status);
    }
}
