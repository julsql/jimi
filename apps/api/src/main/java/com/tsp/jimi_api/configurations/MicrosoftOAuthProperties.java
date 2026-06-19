package com.tsp.jimi_api.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth 2.0 client config for Microsoft (Outlook / Microsoft 365), bound from
 * {@code oauth.microsoft.*}. Mirrors {@link GoogleOAuthProperties}.
 *
 * <p>Uses the Microsoft identity platform v2.0 endpoints. Scope is minimal:
 * read/write calendar events ({@code Calendars.ReadWrite}) plus
 * {@code offline_access} to obtain a refresh token. Register the app in Entra
 * ID (Azure AD) → App registrations; see {@code docs/microsoft-calendar-setup.md}.
 */
@Configuration
@ConfigurationProperties(prefix = "oauth.microsoft")
public class MicrosoftOAuthProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "";
    private String appReturnUrl = "jimi://connected";
    /** "common" works for personal + work accounts; or a specific tenant id. */
    private String tenant = "common";
    private String authUri = "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize";
    private String tokenUri = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private String scope = "offline_access Calendars.ReadWrite";

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !redirectUri.isBlank();
    }

    public String getAuthUri() {
        return String.format(authUri, tenant);
    }

    public String getTokenUri() {
        return String.format(tokenUri, tenant);
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

    public String getTenant() {
        return tenant;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    public void setAuthUri(final String authUri) {
        this.authUri = authUri;
    }

    public void setTokenUri(final String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }
}
