package com.tsp.jimi_api.services.calendar.microsoft;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;

/**
 * Thin authenticated HTTP layer over the Microsoft Graph calendar API. Pulls a
 * valid (auto-refreshed) access token from {@link MicrosoftTokenStore} for every
 * call. Uses RestTemplate to match the project's style.
 */
@Component
public class MicrosoftApiClient {

    private static final String BASE = "https://graph.microsoft.com/v1.0/me";

    private final MicrosoftTokenStore tokens;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String defaultTimezone = ZoneId.systemDefault().getId();

    public MicrosoftApiClient(final MicrosoftTokenStore tokens) {
        this.tokens = tokens;
    }

    /** Events overlapping [from, to] via calendarView, returned in the user's tz. */
    public JSONObject listEvents(final String userId, final String fromIso, final String toIso) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE + "/calendarView")
                .queryParam("startDateTime", fromIso)
                .queryParam("endDateTime", toIso)
                .queryParam("$orderby", "start/dateTime")
                .queryParam("$top", 250)
                .toUriString();
        return new JSONObject(exchange(userId, HttpMethod.GET, url, null));
    }

    public JSONObject insertEvent(final String userId, final JSONObject event) {
        return new JSONObject(exchange(userId, HttpMethod.POST, BASE + "/events", event.toString()));
    }

    public JSONObject patchEvent(final String userId, final String eventId, final JSONObject patch) {
        return new JSONObject(exchange(userId, HttpMethod.PATCH,
                BASE + "/events/" + eventId, patch.toString()));
    }

    public void deleteEvent(final String userId, final String eventId) {
        exchange(userId, HttpMethod.DELETE, BASE + "/events/" + eventId, null);
    }

    private String exchange(final String userId, final HttpMethod method,
                            final String url, final String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.validAccessToken(userId));
        // Ask Graph to return calendarView times in the user's timezone rather
        // than UTC, so the wall-clock hour shown to the user is correct.
        headers.add("Prefer", "outlook.timezone=\"" + defaultTimezone + "\"");
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        ResponseEntity<String> response = restTemplate.exchange(
                url, method, new HttpEntity<>(body, headers), String.class);
        return response.getBody();
    }
}
