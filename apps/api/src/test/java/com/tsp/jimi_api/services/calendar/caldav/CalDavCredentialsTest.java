package com.tsp.jimi_api.services.calendar.caldav;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalDavCredentialsTest {

    @Test
    void roundTripsThroughJson() throws Exception {
        CalDavCredentials creds = new CalDavCredentials(
                "https://caldav.icloud.com/123/calendars/home/", "me@icloud.com", "app-pw");

        CalDavCredentials parsed = CalDavCredentials.fromJson(creds.toJson());

        assertThat(parsed.server()).isEqualTo("https://caldav.icloud.com/123/calendars/home/");
        assertThat(parsed.username()).isEqualTo("me@icloud.com");
        assertThat(parsed.password()).isEqualTo("app-pw");
    }

    @Test
    void normalisesCollectionUrlToASingleTrailingSlash() {
        assertThat(new CalDavCredentials("https://x/home", "u", "p").collectionUrl())
                .isEqualTo("https://x/home/");
        assertThat(new CalDavCredentials("https://x/home/", "u", "p").collectionUrl())
                .isEqualTo("https://x/home/");
    }
}
