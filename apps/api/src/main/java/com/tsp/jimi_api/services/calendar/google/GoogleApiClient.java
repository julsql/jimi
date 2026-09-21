package com.tsp.jimi_api.services.calendar.google;

import com.tsp.jimi_api.services.CalendarAccountService;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Thin authenticated HTTP layer over the Google Calendar v3 REST API.
 *
 * <p>Pulls a valid (auto-refreshed) access token from
 * {@link CalendarAccountService} for every call, so callers never deal with
 * tokens. Uses RestTemplate to match the project's existing HTTP style.
 */
@Component
public class GoogleApiClient {

    private static final String BASE = "https://www.googleapis.com/calendar/v3";

    private final CalendarAccountService accounts;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleApiClient(final CalendarAccountService accounts) {
        this.accounts = accounts;
    }

    /** GET primary calendar events between two RFC3339 instants, time-ordered. */
    public JSONObject listEvents(final String userId, final String timeMin, final String timeMax) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE + "/calendars/primary/events")
                .queryParam("timeMin", timeMin)
                .queryParam("timeMax", timeMax)
                .queryParam("singleEvents", true)
                .queryParam("orderBy", "startTime")
                .queryParam("maxResults", 250)
                .toUriString();
        return new JSONObject(exchange(userId, HttpMethod.GET, url, null));
    }

    public JSONObject insertEvent(final String userId, final JSONObject event) {
        return new JSONObject(exchange(userId, HttpMethod.POST,
                BASE + "/calendars/primary/events", event.toString()));
    }

    public JSONObject patchEvent(final String userId, final String eventId, final JSONObject patch) {
        return new JSONObject(exchange(userId, HttpMethod.PATCH,
                BASE + "/calendars/primary/events/" + eventId, patch.toString()));
    }

    public void deleteEvent(final String userId, final String eventId) {
        exchange(userId, HttpMethod.DELETE, BASE + "/calendars/primary/events/" + eventId, null);
    }

    /** Returns the primary calendar id, which for Google is the account email. */
    public String primaryCalendarEmail(final String userId) {
        JSONObject cal = new JSONObject(exchange(userId, HttpMethod.GET,
                BASE + "/calendars/primary", null));
        return cal.optString("id", null);
    }

    private String exchange(final String userId, final HttpMethod method,
                            final String url, final String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accounts.validAccessToken(userId, CalendarAccountService.GOOGLE));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        ResponseEntity<String> response = restTemplate.exchange(
                url, method, new HttpEntity<>(body, headers), String.class);
        return response.getBody();
    }
}
