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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** Pulls each <calendar-data> blob out of a multistatus REPORT response. */
    private static final Pattern CALENDAR_DATA = Pattern.compile(
            "<(?:[A-Za-z0-9]+:)?calendar-data[^>]*>(.*?)</(?:[A-Za-z0-9]+:)?calendar-data>",
            Pattern.DOTALL);

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

    /** Extracts and unescapes every VEVENT from a multistatus REPORT body. */
    List<CalendarEvent> parseReport(final String xml) {
        List<CalendarEvent> events = new ArrayList<>();
        if (xml == null) {
            return events;
        }
        Matcher m = CALENDAR_DATA.matcher(xml);
        while (m.find()) {
            String ical = unescapeXml(m.group(1)).trim();
            if (ical.contains("BEGIN:VEVENT")) {
                events.add(CalDavICalMapper.toCalendarEvent(ical));
            }
        }
        return events;
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
