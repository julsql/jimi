package com.tsp.jimi_api.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth 2.0 client config for Google Calendar, bound from {@code oauth.google.*}.
 *
 * <p>Set {@code client-id}/{@code client-secret} from your Google Cloud OAuth
 * client (see {@code docs/google-calendar-setup.md}). {@code redirect-uri} must
 * match exactly the Authorized redirect URI registered there.
 * {@code app-return-url} is the deep link the user is bounced back to once the
 * account is linked (e.g. {@code jimi://connected}).
 *
 * <p>Scope is the minimal one needed: read/write events only — NOT full account
 * or contacts access.
 */
@Configuration
@ConfigurationProperties(prefix = "oauth.google")
public class GoogleOAuthProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "";
    private String appReturnUrl = "jimi://connected";
    private String authUri = "https://accounts.google.com/o/oauth2/v2/auth";
    private String tokenUri = "https://oauth2.googleapis.com/token";
    private String revokeUri = "https://oauth2.googleapis.com/revoke";
    private String scope = "https://www.googleapis.com/auth/calendar.events";

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !redirectUri.isBlank();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(final String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAppReturnUrl() {
        return appReturnUrl;
    }

    public void setAppReturnUrl(final String appReturnUrl) {
        this.appReturnUrl = appReturnUrl;
    }

    public String getAuthUri() {
        return authUri;
    }

    public void setAuthUri(final String authUri) {
        this.authUri = authUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(final String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getRevokeUri() {
        return revokeUri;
    }

    public void setRevokeUri(final String revokeUri) {
        this.revokeUri = revokeUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }
}
