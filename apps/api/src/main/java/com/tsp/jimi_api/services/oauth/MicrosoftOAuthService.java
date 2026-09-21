package com.tsp.jimi_api.services.oauth;

import com.tsp.jimi_api.configurations.MicrosoftOAuthProperties;
import com.tsp.jimi_api.services.CalendarAccountService;
import com.tsp.jimi_api.services.calendar.microsoft.MicrosoftTokenStore;
import com.tsp.jimi_api.services.oauth.MicrosoftTokenClient.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Drives the Microsoft OAuth 2.0 authorization-code flow (with PKCE). Mirrors
 * {@link GoogleOAuthService}, reusing the shared {@link OAuthStateCodec} for the
 * encrypted, stateless state + PKCE verifier.
 */
@Service
public class MicrosoftOAuthService {

    private final MicrosoftOAuthProperties props;
    private final OAuthStateCodec stateCodec;
    private final MicrosoftTokenClient tokenClient;
    private final CalendarAccountService accounts;
    private final OAuthReturnPolicy returnPolicy;

    public MicrosoftOAuthService(final MicrosoftOAuthProperties props,
                                 final OAuthStateCodec stateCodec,
                                 final MicrosoftTokenClient tokenClient,
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
                .queryParam("response_mode", "query")
                .queryParam("scope", props.getScope())
                .queryParam("state", issued.state())
                .queryParam("code_challenge", issued.codeChallenge())
                .queryParam("code_challenge_method", "S256")
                .encode()
                .toUriString();
    }

    public String handleCallback(final String code, final String state) {
        OAuthStateCodec.StatePayload payload = stateCodec.verify(state);
        TokenResponse tokens = tokenClient.exchangeCode(code, payload.codeVerifier());
        accounts.link(payload.userId(), MicrosoftTokenStore.MICROSOFT,
                toGeneric(tokens), null);
        return returnPolicy.resolve(payload.returnUrl(), props.getAppReturnUrl(), "connected");
    }

    public String appReturn(final String status) {
        return returnPolicy.resolve(null, props.getAppReturnUrl(), status);
    }

    // Adapts the Microsoft token record to the shared one CalendarAccountService
    // stores (both carry the same fields).
    private GoogleTokenClient.TokenResponse toGeneric(final TokenResponse t) {
        return new GoogleTokenClient.TokenResponse(
                t.accessToken(), t.refreshToken(), t.expiry(), t.scope());
    }
}
