package com.tsp.jimi_api.services.calendar.caldav;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import com.tsp.jimi_api.services.calendar.CalendarProvider;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link CalendarProvider} backed by any CalDAV server (Apple iCloud, Fastmail,
 * Nextcloud, ...).
 *
 * <p>CalDAV is <strong>not</strong> OAuth: the user supplies a calendar
 * collection URL plus HTTP Basic credentials (username + app-specific
 * password), stored encrypted via {@link CalDavAccountService}. We deliberately
 * skip principal / calendar-home discovery — the user provides the full
 * collection URL directly (e.g. iCloud
 * {@code https://caldav.icloud.com/<id>/calendars/home/}).
 *
 * <p>iCalendar build/parse lives in {@link CalDavICalMapper} (no I/O); this
 * class only wires it to {@link CalDavClient}.
 */
@Component
public class CalDavCalendarProvider implements CalendarProvider {

    /** Local name of the CalDAV element carrying each iCalendar blob. */
    private static final String CALENDAR_DATA = "calendar-data";

    private final CalDavAccountService accounts;
    private final CalDavClient client;

    public CalDavCalendarProvider(final CalDavAccountService accounts, final CalDavClient client) {
        this.accounts = accounts;
        this.client = client;
    }

    @Override
    public String id() {
        return CalDavAccountService.CALDAV;
    }

    @Override
    public boolean isConnected(final String userId) {
        return accounts.isConnected(userId);
    }

    @Override
    public List<CalendarEvent> findEvents(final String userId, final String fromIso, final String toIso) {
        CalDavCredentials creds = accounts.credentials(userId);
        String xml = client.reportTimeRange(creds, toUtcStamp(fromIso), toUtcStamp(toIso));
        return parseReport(xml);
    }

    @Override
    public CalendarEvent create(final String userId, final EventDraft draft) {
        CalDavCredentials creds = accounts.credentials(userId);
        String uid = CalDavICalMapper.newUid();
        String ical = CalDavICalMapper.toICalendar(draft, uid);
        client.put(creds, uid, ical);
        return CalDavICalMapper.toCalendarEvent(ical);
    }

    @Override
    public CalendarEvent update(final String userId, final String eventId, final EventDraft changes) {
        CalDavCredentials creds = accounts.credentials(userId);
        String existing = client.get(creds, eventId);
        String updated = CalDavICalMapper.applyChanges(existing, changes);
        client.put(creds, eventId, updated);
        return CalDavICalMapper.toCalendarEvent(updated);
    }

    @Override
    public void delete(final String userId, final String eventId) {
        client.delete(accounts.credentials(userId), eventId);
    }

    // --- helpers -----------------------------------------------------------

    /**
     * Extracts and unescapes every VEVENT from a multistatus REPORT body.
     *
     * <p>This used to rely on the regex
     * {@code <(?:[A-Za-z0-9]+:)?calendar-data[^>]*>(.*?)</(?:[A-Za-z0-9]+:)?calendar-data>}.
     * That pattern is vulnerable to polynomial ReDoS (CodeQL
     * {@code java/polynomial-redos}): {@code xml} comes straight off the wire
     * from the (user-configured) CalDAV server, and a body made of many
     * {@code <calendar-data} fragments with no terminating {@code >} forces the
     * engine to rescan the tail from every start position — O(n²) work.
     *
     * <p>The scanner below does the same job with a single linear forward pass
     * using {@link String#indexOf} plus constant-size boundary checks. There is
     * no backtracking and no regex, so matching is strictly O(n) and cannot be
     * driven super-linear by hostile input.
     */
    List<CalendarEvent> parseReport(final String xml) {
        List<CalendarEvent> events = new ArrayList<>();
        if (xml == null) {
            return events;
        }
        int cursor = 0;
        while (cursor < xml.length()) {
            // Start of the content right after a "<[prefix:]calendar-data ...>" tag.
            int contentStart = openTagContentStart(xml, cursor);
            if (contentStart < 0) {
                break;
            }
            // Its matching "</[prefix:]calendar-data>" closing tag.
            int closeTagStart = closeTagStart(xml, contentStart);
            if (closeTagStart < 0) {
                break;
            }
            String ical = unescapeXml(xml.substring(contentStart, closeTagStart)).trim();
            if (ical.contains("BEGIN:VEVENT")) {
                events.add(CalDavICalMapper.toCalendarEvent(ical));
            }
            int closeEnd = xml.indexOf('>', closeTagStart);
            if (closeEnd < 0) {
                break;
            }
            cursor = closeEnd + 1;
        }
        return events;
    }

    /**
     * Index of the first character after the next opening tag
     * {@code <[A-Za-z0-9]+:?calendar-data[^>]*>} at or after {@code from}, or
     * {@code -1} if there is none. Linear forward scan, no backtracking.
     */
    private static int openTagContentStart(final String xml, final int from) {
        for (int i = xml.indexOf(CALENDAR_DATA, from); i >= 0;
                i = xml.indexOf(CALENDAR_DATA, i + CALENDAR_DATA.length())) {
            if (openTagLeftBoundary(xml, i) >= 0) {
                int gt = xml.indexOf('>', i + CALENDAR_DATA.length());
                // No '>' ahead means no complete tag can follow either: stop.
                return gt < 0 ? -1 : gt + 1;
            }
        }
        return -1;
    }

    /**
     * Index of the {@code '<'} of the next closing tag
     * {@code </[A-Za-z0-9]+:?calendar-data>} at or after {@code from}, or
     * {@code -1}. Linear forward scan, no backtracking.
     */
    private static int closeTagStart(final String xml, final int from) {
        for (int i = xml.indexOf(CALENDAR_DATA, from); i >= 0;
                i = xml.indexOf(CALENDAR_DATA, i + CALENDAR_DATA.length())) {
            int after = i + CALENDAR_DATA.length();
            if (after < xml.length() && xml.charAt(after) == '>') {
                int lt = closeTagLeftBoundary(xml, i);
                if (lt >= 0) {
                    return lt;
                }
            }
        }
        return -1;
    }

    /**
     * If {@code calendar-data} at {@code dataIdx} is preceded by {@code '<'} or
     * {@code "<[A-Za-z0-9]+:"}, returns the index of that {@code '<'}; else -1.
     */
    private static int openTagLeftBoundary(final String xml, final int dataIdx) {
        if (dataIdx >= 1 && xml.charAt(dataIdx - 1) == '<') {
            return dataIdx - 1;
        }
        if (dataIdx >= 2 && xml.charAt(dataIdx - 1) == ':') {
            int j = dataIdx - 2;
            while (j >= 0 && isAlnum(xml.charAt(j))) {
                j--;
            }
            if (j < dataIdx - 2 && j >= 0 && xml.charAt(j) == '<') {
                return j;
            }
        }
        return -1;
    }

    /**
     * If {@code calendar-data} at {@code dataIdx} is preceded by {@code "</"} or
     * {@code "</[A-Za-z0-9]+:"}, returns the index of that {@code '<'}; else -1.
     */
    private static int closeTagLeftBoundary(final String xml, final int dataIdx) {
        if (dataIdx >= 2 && xml.charAt(dataIdx - 1) == '/' && xml.charAt(dataIdx - 2) == '<') {
            return dataIdx - 2;
        }
        if (dataIdx >= 4 && xml.charAt(dataIdx - 1) == ':') {
            int j = dataIdx - 2;
            while (j >= 0 && isAlnum(xml.charAt(j))) {
                j--;
            }
            if (j < dataIdx - 2 && j >= 1 && xml.charAt(j) == '/' && xml.charAt(j - 1) == '<') {
                return j - 1;
            }
        }
        return -1;
    }

    private static boolean isAlnum(final char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    /** CalDAV time-range bounds must be UTC "yyyyMMdd'T'HHmmss'Z'". */
    private static String toUtcStamp(final String iso) {
        OffsetDateTime odt = OffsetDateTime.parse(iso).withOffsetSameInstant(ZoneOffset.UTC);
        return odt.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }

    private static String unescapeXml(final String s) {
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}
