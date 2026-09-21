package com.tsp.jimi_api.services.calendar.google;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure translation between JIMI's provider-neutral models and the Google
 * Calendar v3 event JSON. No I/O — kept separate so it is fully unit-testable.
 *
 * @see <a href="https://developers.google.com/calendar/api/v3/reference/events">Events resource</a>
 */
public final class GoogleEventMapper {

    private GoogleEventMapper() {
    }

    /** Builds the request body for events.insert / events.patch from a draft. */
    public static JSONObject toGoogleEvent(final EventDraft draft, final String defaultTimezone) {
        JSONObject event = new JSONObject();
        putIfPresent(event, "summary", draft.title());
        putIfPresent(event, "location", draft.location());
        putIfPresent(event, "description", draft.description());

        String timezone = draft.timezone() != null ? draft.timezone() : defaultTimezone;
        boolean allDay = draft.isAllDay() || isDateOnly(draft.start());
        if (draft.start() != null) {
            event.put("start", timePoint(draft.start(), allDay, timezone));
        }
        if (draft.end() != null) {
            event.put("end", timePoint(draft.end(), allDay, timezone));
        }

        if (!draft.attendees().isEmpty()) {
            JSONArray attendees = new JSONArray();
            for (String email : draft.attendees()) {
                attendees.put(new JSONObject().put("email", email));
            }
            event.put("attendees", attendees);
        }

        if (draft.recurrence() != null) {
            event.put("recurrence", new JSONArray().put(draft.recurrence()));
        }

        if (draft.reminderMinutes() != null) {
            event.put("reminders", new JSONObject()
                    .put("useDefault", false)
                    .put("overrides", new JSONArray().put(
                            new JSONObject().put("method", "popup").put("minutes", draft.reminderMinutes()))));
        }
        return event;
    }

    /** Maps a Google event resource to JIMI's read model. */
    public static CalendarEvent toCalendarEvent(final JSONObject event) {
        boolean allDay = event.optJSONObject("start") != null
                && event.getJSONObject("start").has("date");
        return new CalendarEvent(
                event.optString("id", null),
                event.optString("summary", null),
                readTimePoint(event.optJSONObject("start")),
                readTimePoint(event.optJSONObject("end")),
                allDay,
                event.has("location") ? event.optString("location", null) : null,
                readAttendees(event.optJSONArray("attendees")),
                event.has("description") ? event.optString("description", null) : null,
                readRecurrence(event.optJSONArray("recurrence")),
                event.has("htmlLink") ? event.optString("htmlLink", null) : null);
    }

    private static JSONObject timePoint(final String iso, final boolean allDay, final String timezone) {
        if (allDay) {
            return new JSONObject().put("date", datePart(iso));
        }
        JSONObject point = new JSONObject().put("dateTime", withSeconds(iso));
        if (timezone != null) {
            point.put("timeZone", timezone);
        }
        return point;
    }

    private static String readTimePoint(final JSONObject point) {
        if (point == null) {
            return null;
        }
        if (point.has("dateTime")) {
            return point.optString("dateTime", null);
        }
        return point.optString("date", null);
    }

    private static List<String> readAttendees(final JSONArray array) {
        List<String> out = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String email = array.getJSONObject(i).optString("email", "");
                if (!email.isEmpty()) {
                    out.add(email);
                }
            }
        }
        return out;
    }

    private static String readRecurrence(final JSONArray array) {
        return array != null && array.length() > 0 ? array.getString(0) : null;
    }

    private static boolean isDateOnly(final String iso) {
        return iso != null && !iso.contains("T");
    }

    private static String datePart(final String iso) {
        int t = iso.indexOf('T');
        return t > 0 ? iso.substring(0, t) : iso;
    }

    /** Google's dateTime is RFC3339; "2026-06-20T13:00" needs ":00" seconds. */
    private static String withSeconds(final String iso) {
        // count colons: "HH:mm" → 1 colon after the date's T part
        int t = iso.indexOf('T');
        if (t < 0) {
            return iso;
        }
        String time = iso.substring(t + 1);
        if (time.length() == 5) {
            return iso + ":00";
        }
        return iso;
    }

    private static void putIfPresent(final JSONObject json, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            json.put(key, value);
        }
    }
}
