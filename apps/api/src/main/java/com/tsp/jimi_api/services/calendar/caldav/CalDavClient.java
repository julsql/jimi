package com.tsp.jimi_api.services.calendar.caldav;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Thin HTTP layer over a CalDAV calendar <em>collection</em>, using Spring's
 * {@link RestTemplate} to match the project's existing style. CalDAV verbs that
 * {@link HttpMethod} has no constant for (PROPFIND, REPORT) are issued via
 * {@link HttpMethod#valueOf(String)}.
 *
 * <p>Auth is HTTP Basic (username + app-specific password); there is no token
 * to refresh. Each method takes the {@link CalDavCredentials} so the client
 * stays stateless and unit-friendly.
 */
@Component
public class CalDavClient {

    private static final HttpMethod PROPFIND = HttpMethod.valueOf("PROPFIND");
    private static final HttpMethod REPORT = HttpMethod.valueOf("REPORT");

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Validates credentials by issuing a {@code PROPFIND Depth: 0} against the
     * collection URL. Returns true on any 2xx response.
     */
    public boolean validate(final CalDavCredentials creds) {
        HttpHeaders headers = baseHeaders(creds);
        headers.set("Depth", "0");
        String body = """
                <?xml version="1.0" encoding="utf-8"?>
                <d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/></d:prop></d:propfind>""";
        ResponseEntity<String> response = restTemplate.exchange(
                creds.collectionUrl(), PROPFIND,
                new HttpEntity<>(body, xml(headers)), String.class);
        return response.getStatusCode().is2xxSuccessful();
    }

    /**
     * Runs a {@code calendar-query REPORT} with a VEVENT time-range filter and
     * returns the raw multistatus XML body (containing the matched .ics data).
     */
    public String reportTimeRange(final CalDavCredentials creds,
                                  final String startUtc, final String endUtc) {
        HttpHeaders headers = baseHeaders(creds);
        headers.set("Depth", "1");
        String body = """
                <?xml version="1.0" encoding="utf-8"?>
                <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <d:prop><d:getetag/><c:calendar-data/></d:prop>
                  <c:filter>
                    <c:comp-filter name="VCALENDAR">
                      <c:comp-filter name="VEVENT">
                        <c:time-range start="%s" end="%s"/>
                      </c:comp-filter>
                    </c:comp-filter>
                  </c:filter>
                </c:calendar-query>""".formatted(startUtc, endUtc);
        ResponseEntity<String> response = restTemplate.exchange(
                creds.collectionUrl(), REPORT,
                new HttpEntity<>(body, xml(headers)), String.class);
        return response.getBody();
    }

    /** PUTs an iCalendar document to {@code <collection>/<uid>.ics}. */
    public void put(final CalDavCredentials creds, final String uid, final String ical) {
        HttpHeaders headers = baseHeaders(creds);
        headers.setContentType(MediaType.parseMediaType("text/calendar; charset=utf-8"));
        restTemplate.exchange(resourceUrl(creds, uid), HttpMethod.PUT,
                new HttpEntity<>(ical, headers), String.class);
    }

    /** GETs the raw .ics for an event. */
    public String get(final CalDavCredentials creds, final String uid) {
        ResponseEntity<String> response = restTemplate.exchange(
                resourceUrl(creds, uid), HttpMethod.GET,
                new HttpEntity<>(baseHeaders(creds)), String.class);
        return response.getBody();
    }

    /** DELETEs {@code <collection>/<uid>.ics}. */
    public void delete(final CalDavCredentials creds, final String uid) {
        restTemplate.exchange(resourceUrl(creds, uid), HttpMethod.DELETE,
                new HttpEntity<>(baseHeaders(creds)), String.class);
    }

    private String resourceUrl(final CalDavCredentials creds, final String uid) {
        String id = uid.endsWith(".ics") ? uid : uid + ".ics";
        return creds.collectionUrl() + id;
    }

    private HttpHeaders baseHeaders(final CalDavCredentials creds) {
        HttpHeaders headers = new HttpHeaders();
        String token = Base64.getEncoder().encodeToString(
                (creds.username() + ":" + creds.password()).getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + token);
        headers.setAccept(List.of(MediaType.ALL));
        return headers;
    }

    private HttpHeaders xml(final HttpHeaders headers) {
        headers.setContentType(MediaType.parseMediaType("application/xml; charset=utf-8"));
        return headers;
    }
}
