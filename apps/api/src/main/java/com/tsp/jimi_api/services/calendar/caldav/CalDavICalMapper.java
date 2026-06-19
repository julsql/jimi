package com.tsp.jimi_api.services.calendar.caldav;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure translation between JIMI's provider-neutral models and the iCalendar
 * (RFC 5545) VEVENT text used by CalDAV. No HTTP — kept separate so it is fully
 * unit-testable.
 *
 * <p>Field mapping:
 * <ul>
 *   <li>SUMMARY      ↔ title</li>
 *   <li>DTSTART/DTEND ↔ start/end (all-day uses {@code VALUE=DATE}, timed uses
 *       a local {@code DATE-TIME} so the calendar shows the wall-clock time)</li>
 *   <li>LOCATION     ↔ location</li>
 *   <li>DESCRIPTION  ↔ description</li>
 *   <li>ATTENDEE     ↔ attendees (mailto:)</li>
 *   <li>RRULE        ↔ recurrence</li>
 *   <li>UID          ↔ id</li>
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545">RFC 5545</a>
 */
public final class CalDavICalMapper {

    private static final String CRLF = "\r\n";

    private CalDavICalMapper() {
    }

    /**
     * Builds a full VCALENDAR/VEVENT document for a {@code PUT}. {@code uid} is
     * the event identifier (also used as the {@code <uid>.ics} resource name).
     */
    public static String toICalendar(final EventDraft draft, final String uid) {
        boolean allDay = draft.isAllDay() || isDateOnly(draft.start());

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR").append(CRLF);
        sb.append("VERSION:2.0").append(CRLF);
        sb.append("PRODID:-//JIMI//CalDAV//EN").append(CRLF);
        sb.append("BEGIN:VEVENT").append(CRLF);
        sb.append("UID:").append(uid).append(CRLF);

        appendIfPresent(sb, "SUMMARY", draft.title());
        appendDate(sb, "DTSTART", draft.start(), allDay);
        appendDate(sb, "DTEND", draft.end(), allDay);
        appendIfPresent(sb, "LOCATION", draft.location());
        appendIfPresent(sb, "DESCRIPTION", draft.description());

        if (draft.recurrence() != null) {
            sb.append(stripRrulePrefix(draft.recurrence())).append(CRLF);
        }
        for (String attendee : draft.attendees()) {
            sb.append("ATTENDEE:mailto:").append(attendee).append(CRLF);
        }

        sb.append("END:VEVENT").append(CRLF);
        sb.append("END:VCALENDAR").append(CRLF);
        return sb.toString();
    }

    /**
     * Applies the non-null fields of {@code changes} on top of an existing
     * iCalendar document (read back from the server) and returns the new
     * document, preserving the original UID and any untouched properties.
     */
    public static String applyChanges(final String existingICal, final EventDraft changes) {
        CalendarEvent current = toCalendarEvent(existingICal);
        String uid = current.id() != null ? current.id() : newUid();

        boolean allDay = changes.allDay() != null ? changes.isAllDay() : current.allDay();
        String start = firstNonNull(changes.start(), current.start());
        String end = firstNonNull(changes.end(), current.end());

        EventDraft merged = new EventDraft(
                firstNonNull(changes.title(), current.title()),
                start,
                end,
                allDay,
                changes.timezone(),
                firstNonNull(changes.location(), current.location()),
                changes.attendees().isEmpty() ? current.attendees() : changes.attendees(),
                firstNonNull(changes.description(), current.description()),
                firstNonNull(changes.recurrence(), current.recurrence()),
                changes.reminderMinutes(),
                changes.type());
        return toICalendar(merged, uid);
    }

    /** Parses a single VEVENT out of an iCalendar document into the read model. */
    public static CalendarEvent toCalendarEvent(final String ical) {
        List<String> lines = unfold(ical);

        String uid = null;
        String summary = null;
        String location = null;
        String description = null;
        String recurrence = null;
        String start = null;
        String end = null;
        boolean allDay = false;
        List<String> attendees = new ArrayList<>();

        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String name = line.substring(0, colon);
            String value = line.substring(colon + 1);
            String propName = name.contains(";") ? name.substring(0, name.indexOf(';')) : name;

            switch (propName.toUpperCase()) {
                case "UID" -> uid = value;
                case "SUMMARY" -> summary = unescape(value);
                case "LOCATION" -> location = unescape(value);
                case "DESCRIPTION" -> description = unescape(value);
                case "RRULE" -> recurrence = "RRULE:" + value;
                case "ATTENDEE" -> {
                    String email = value.toLowerCase().startsWith("mailto:") ? value.substring(7) : value;
                    if (!email.isBlank()) {
                        attendees.add(email);
                    }
                }
                case "DTSTART" -> {
                    boolean date = isDateValue(name);
                    allDay = allDay || date;
                    start = parseIcalDate(value, date);
                }
                case "DTEND" -> end = parseIcalDate(value, isDateValue(name));
                default -> { /* ignore other props */ }
            }
        }

        return new CalendarEvent(uid, summary, start, end, allDay, location,
                attendees, description, recurrence, null);
    }

    /** Generates a fresh UID suitable for the {@code <uid>.ics} resource name. */
    public static String newUid() {
        return java.util.UUID.randomUUID().toString();
    }

    // --- helpers -----------------------------------------------------------

    private static void appendIfPresent(final StringBuilder sb, final String prop, final String value) {
        if (value != null && !value.isBlank()) {
            sb.append(prop).append(':').append(escape(value)).append(CRLF);
        }
    }

    private static void appendDate(final StringBuilder sb, final String prop,
                                   final String iso, final boolean allDay) {
        if (iso == null) {
            return;
        }
        if (allDay) {
            sb.append(prop).append(";VALUE=DATE:").append(datePart(iso).replace("-", "")).append(CRLF);
        } else {
            sb.append(prop).append(':').append(toIcalDateTime(iso)).append(CRLF);
        }
    }

    /** "2026-06-20T13:00" → "20260620T130000" (local time, no Z). */
    private static String toIcalDateTime(final String iso) {
        int t = iso.indexOf('T');
        if (t < 0) {
            return iso.replace("-", "") + "T000000";
        }
        String date = iso.substring(0, t).replace("-", "");
        String time = iso.substring(t + 1);
        // strip any offset / Z suffix — we keep wall-clock time
        int cut = time.length();
        for (int i = 0; i < time.length(); i++) {
            char c = time.charAt(i);
            if (c == '+' || c == '-' || c == 'Z') {
                cut = i;
                break;
            }
        }
        time = time.substring(0, cut).replace(":", "");
        if (time.length() == 4) {
            time = time + "00"; // HHmm → HHmmss
        } else if (time.length() < 6) {
            time = (time + "000000").substring(0, 6);
        } else if (time.length() > 6) {
            time = time.substring(0, 6);
        }
        return date + "T" + time;
    }

    /** "20260620" → "2026-06-20"; "20260620T130000(Z)" → "2026-06-20T13:00:00". */
    private static String parseIcalDate(final String value, final boolean dateOnly) {
        String v = value.trim();
        if (dateOnly || !v.contains("T")) {
            String d = v.length() >= 8 ? v.substring(0, 8) : v;
            return d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6, 8);
        }
        int t = v.indexOf('T');
        String d = v.substring(0, t);
        String time = v.substring(t + 1).replace("Z", "");
        String date = d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6, 8);
        String hh = time.length() >= 2 ? time.substring(0, 2) : "00";
        String mm = time.length() >= 4 ? time.substring(2, 4) : "00";
        String ss = time.length() >= 6 ? time.substring(4, 6) : "00";
        return date + "T" + hh + ":" + mm + ":" + ss;
    }

    private static boolean isDateValue(final String propName) {
        return propName.toUpperCase().contains("VALUE=DATE")
                && !propName.toUpperCase().contains("DATE-TIME");
    }

    private static boolean isDateOnly(final String iso) {
        return iso != null && !iso.contains("T");
    }

    private static String datePart(final String iso) {
        int t = iso.indexOf('T');
        return t > 0 ? iso.substring(0, t) : iso;
    }

    private static String stripRrulePrefix(final String recurrence) {
        String r = recurrence.trim();
        return r.toUpperCase().startsWith("RRULE:") ? r : "RRULE:" + r;
    }

    /** Unfolds RFC 5545 line continuations (CRLF followed by space/tab). */
    private static List<String> unfold(final String ical) {
        List<String> out = new ArrayList<>();
        for (String raw : ical.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1)) {
            if ((raw.startsWith(" ") || raw.startsWith("\t")) && !out.isEmpty()) {
                int last = out.size() - 1;
                out.set(last, out.get(last) + raw.substring(1));
            } else {
                out.add(raw);
            }
        }
        return out;
    }

    private static String escape(final String value) {
        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }

    private static String unescape(final String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 'n', 'N' -> sb.append('\n');
                    case ',' -> sb.append(',');
                    case ';' -> sb.append(';');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String firstNonNull(final String a, final String b) {
        return a != null ? a : b;
    }
}
