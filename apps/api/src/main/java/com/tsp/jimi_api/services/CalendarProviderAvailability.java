package com.tsp.jimi_api.services;

import com.tsp.jimi_api.configurations.CalendarProvidersProperties;
import com.tsp.jimi_api.configurations.GoogleOAuthProperties;
import com.tsp.jimi_api.configurations.MicrosoftOAuthProperties;
import com.tsp.jimi_api.services.calendar.caldav.CalDavAccountService;
import com.tsp.jimi_api.services.calendar.microsoft.MicrosoftTokenStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single source of truth for whether a calendar provider is actually offered.
 *
 * <p>A provider is available only when it is BOTH enabled by deployment
 * ({@link CalendarProvidersProperties}) AND, for OAuth providers, configured
 * with client credentials. CalDAV has no server-side credentials (the user
 * supplies them), so it depends on the enabled flag alone.
 *
 * <p>Consumed by {@code GET /config} (so the app hides disabled options) and by
 * the {@code /connect/*} controllers (so a disabled provider cannot be linked).
 */
@Service
public class CalendarProviderAvailability {

    private final CalendarProvidersProperties enabled;
    private final GoogleOAuthProperties google;
    private final MicrosoftOAuthProperties microsoft;

    public CalendarProviderAvailability(final CalendarProvidersProperties enabled,
                                        final GoogleOAuthProperties google,
                                        final MicrosoftOAuthProperties microsoft) {
        this.enabled = enabled;
        this.google = google;
        this.microsoft = microsoft;
    }

    public boolean googleAvailable() {
        return enabled.isGoogle() && google.isConfigured();
    }

    public boolean microsoftAvailable() {
        return enabled.isMicrosoft() && microsoft.isConfigured();
    }

    public boolean caldavAvailable() {
        return enabled.isCaldav();
    }

    /** Provider → availability, in display order. Backs {@code GET /config}. */
    public Map<String, Boolean> snapshot() {
        Map<String, Boolean> providers = new LinkedHashMap<>();
        providers.put(CalendarAccountService.GOOGLE, googleAvailable());
        providers.put(MicrosoftTokenStore.MICROSOFT, microsoftAvailable());
        providers.put(CalDavAccountService.CALDAV, caldavAvailable());
        return providers;
    }
}
