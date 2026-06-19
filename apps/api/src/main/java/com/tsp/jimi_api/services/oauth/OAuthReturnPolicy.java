package com.tsp.jimi_api.services.oauth;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds the URL the user's browser/app is bounced to after an OAuth callback.
 *
 * <p>The client supplies its own return target so the same flow works for the
 * native app (a custom-scheme deep link like {@code jimi://connected}) and the
 * web app (its own https origin, e.g. {@code https://jimi.julsql.fr/connected}).
 * Without this, the server could only redirect to one fixed target — which is
 * why the web app got "Safari cannot open jimi://connected".
 *
 * <p>The redirect carries only a {@code status} flag — never a token or auth
 * code (those are exchanged server-side before the redirect). Still, to avoid
 * being an open redirector we only honour a well-formed custom-scheme or
 * http(s) URL, and otherwise fall back to the server's configured default.
 */
@Component
public class OAuthReturnPolicy {

    /**
     * @param requested client-supplied return URL (may be null/blank/untrusted)
     * @param fallback  the server's configured default (e.g. app-return-url)
     * @param status    "connected" or "error"
     * @return the safe return URL with {@code ?status=...} appended
     */
    public String resolve(final String requested, final String fallback, final String status) {
        String base = isAcceptable(requested) ? requested : fallback;
        return UriComponentsBuilder.fromUriString(base)
                .queryParam("status", status)
                .build()
                .toUriString();
    }

    private boolean isAcceptable(final String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        // scheme://something — web origins (http/https) or a native custom scheme.
        return url.matches("^[a-zA-Z][a-zA-Z0-9+.\\-]*://[^\\s]+$");
    }
}
