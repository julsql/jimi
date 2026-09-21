package com.tsp.jimi_api.records;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Provider-neutral view of a single calendar event, as read back from the
 * user's real calendar (Google, CalDAV, Microsoft...).
 *
 * <p>This is a <em>read</em> model: it is never persisted in JIMI's database.
 * The user's calendar is the single source of truth — JIMI only ever holds
 * an event in memory for the duration of one request.
 *
 * @param id          provider-assigned event id (used to edit/delete)
 * @param title       event summary/title
 * @param start       ISO-8601 start ("2026-06-20T14:00" or "2026-06-20" if all-day)
 * @param end         ISO-8601 end, may be null
 * @param allDay      true for all-day events
 * @param location    free-text location, may be null
 * @param attendees   attendee email addresses, never null (possibly empty)
 * @param description event notes/body, may be null
 * @param recurrence  iCal RRULE string for recurring events, may be null
 * @param url         deep link to open the event in its native calendar app, may be null
 */
public record CalendarEvent(
        String id,
        String title,
        String start,
        String end,
        boolean allDay,
        String location,
        List<String> attendees,
        String description,
        String recurrence,
        String url) {

    public CalendarEvent {
        attendees = attendees == null ? List.of() : List.copyOf(attendees);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("start", start);
        json.put("end", end == null ? JSONObject.NULL : end);
        json.put("allDay", allDay);
        json.put("location", location == null ? JSONObject.NULL : location);
        json.put("attendees", new JSONArray(attendees));
        json.put("description", description == null ? JSONObject.NULL : description);
        json.put("recurrence", recurrence == null ? JSONObject.NULL : recurrence);
        json.put("url", url == null ? JSONObject.NULL : url);
        return json;
    }

    /**
     * Short one-line human description used when asking the user to confirm
     * an edit/delete (never includes data the user did not put there).
     */
    public String describe() {
        StringBuilder sb = new StringBuilder("\"").append(title == null ? "(untitled)" : title).append("\"");
        if (start != null) {
            sb.append(" on ").append(prettyStart());
        }
        if (location != null && !location.isBlank()) {
            sb.append(" at ").append(location);
        }
        return sb.toString();
    }

    /**
     * Wall-clock rendering of {@link #start} ("2026-06-20 13:00"), stripping the
     * RFC3339 offset/seconds. The time portion of a provider dateTime is already
     * in the calendar's own timezone, so we show it verbatim — this avoids the
     * LLM re-converting "13:00:00+02:00" to UTC and reporting the wrong hour.
     */
    public String prettyStart() {
        if (start == null) {
            return "";
        }
        int t = start.indexOf('T');
        if (t < 0) {
            return start; // all-day: date only
        }
        String date = start.substring(0, t);
        String time = start.substring(t + 1);
        String hhmm = time.length() >= 5 ? time.substring(0, 5) : time;
        return date + " " + hhmm;
    }
}
