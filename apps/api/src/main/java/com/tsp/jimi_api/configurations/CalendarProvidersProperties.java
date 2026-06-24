package com.tsp.jimi_api.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Deployment toggles for which calendar providers JIMI offers, bound from
 * {@code calendar.providers.*}.
 *
 * <p>Each flag is a single deploy-time switch (env var
 * {@code GOOGLE_CALENDAR_ENABLED} / {@code MICROSOFT_CALENDAR_ENABLED} /
 * {@code CALDAV_CALENDAR_ENABLED}) that flips a provider off without touching
 * its OAuth credentials. When false, {@code GET /config} stops advertising the
 * provider (so the app hides the option) and the matching {@code /connect/*}
 * endpoint refuses to start a link. Defaults to enabled.
 *
 * <p>This is independent of {@code isConfigured()}: a provider is actually
 * offered only when it is BOTH enabled here AND configured (for OAuth ones).
 * See {@link com.tsp.jimi_api.services.CalendarProviderAvailability}.
 */
@Configuration
@ConfigurationProperties(prefix = "calendar.providers")
public class CalendarProvidersProperties {

    private boolean google = true;
    private boolean microsoft = true;
    private boolean caldav = true;

    public boolean isGoogle() {
        return google;
    }

    public void setGoogle(final boolean google) {
        this.google = google;
    }

    public boolean isMicrosoft() {
        return microsoft;
    }

    public void setMicrosoft(final boolean microsoft) {
        this.microsoft = microsoft;
    }

    public boolean isCaldav() {
        return caldav;
    }

    public void setCaldav(final boolean caldav) {
        this.caldav = caldav;
    }
}
