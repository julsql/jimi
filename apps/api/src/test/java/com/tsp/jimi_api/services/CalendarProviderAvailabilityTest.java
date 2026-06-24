package com.tsp.jimi_api.services;

import com.tsp.jimi_api.configurations.CalendarProvidersProperties;
import com.tsp.jimi_api.configurations.GoogleOAuthProperties;
import com.tsp.jimi_api.configurations.MicrosoftOAuthProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarProviderAvailabilityTest {

    private static GoogleOAuthProperties configuredGoogle() {
        GoogleOAuthProperties p = new GoogleOAuthProperties();
        p.setClientId("id");
        p.setClientSecret("secret");
        p.setRedirectUri("https://api/cb");
        return p;
    }

    private static MicrosoftOAuthProperties configuredMicrosoft() {
        MicrosoftOAuthProperties p = new MicrosoftOAuthProperties();
        p.setClientId("id");
        p.setClientSecret("secret");
        p.setRedirectUri("https://api/cb");
        return p;
    }

    private static CalendarProvidersProperties allEnabled() {
        return new CalendarProvidersProperties(); // defaults: all true
    }

    @Test
    void availableWhenEnabledAndConfigured() {
        var availability = new CalendarProviderAvailability(
                allEnabled(), configuredGoogle(), configuredMicrosoft());

        assertThat(availability.googleAvailable()).isTrue();
        assertThat(availability.microsoftAvailable()).isTrue();
        assertThat(availability.caldavAvailable()).isTrue();
    }

    @Test
    void disabledFlagHidesAnOtherwiseConfiguredProvider() {
        CalendarProvidersProperties enabled = allEnabled();
        enabled.setGoogle(false);

        var availability = new CalendarProviderAvailability(
                enabled, configuredGoogle(), configuredMicrosoft());

        assertThat(availability.googleAvailable()).isFalse();
        assertThat(availability.microsoftAvailable()).isTrue();
    }

    @Test
    void enabledButUnconfiguredOAuthProviderIsStillUnavailable() {
        var availability = new CalendarProviderAvailability(
                allEnabled(), new GoogleOAuthProperties(), new MicrosoftOAuthProperties());

        assertThat(availability.googleAvailable()).isFalse();
        assertThat(availability.microsoftAvailable()).isFalse();
        // CalDAV has no server credentials, so it stays available on the flag alone.
        assertThat(availability.caldavAvailable()).isTrue();
    }

    @Test
    void caldavDependsOnTheFlagAlone() {
        CalendarProvidersProperties enabled = allEnabled();
        enabled.setCaldav(false);

        var availability = new CalendarProviderAvailability(
                enabled, configuredGoogle(), configuredMicrosoft());

        assertThat(availability.caldavAvailable()).isFalse();
    }

    @Test
    void snapshotReportsEveryProviderInOrder() {
        CalendarProvidersProperties enabled = allEnabled();
        enabled.setMicrosoft(false);

        var availability = new CalendarProviderAvailability(
                enabled, configuredGoogle(), configuredMicrosoft());

        Map<String, Boolean> snapshot = availability.snapshot();
        assertThat(snapshot).containsExactly(
                Map.entry("google", true),
                Map.entry("microsoft", false),
                Map.entry("caldav", true));
    }
}
