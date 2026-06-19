package com.tsp.jimi_api.services.calendar.microsoft;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure translation between JIMI's provider-neutral models and Microsoft Graph
 * event JSON. No I/O — fully unit-testable.
 *
 * <p>Recurrence is intentionally not mapped: Graph uses a structured
 * {@code patternedRecurrence} object rather than an iCal RRULE, so recurring
 * events are out of scope for this provider for now (single events only).
 *
 * @see <a href="https://learn.microsoft.com/graph/api/resources/event">Graph event</a>
 */
public final class MicrosoftEventMapper {

    private MicrosoftEventMapper() {
    }

    public static JSONObject toGraphEvent(final EventDraft draft, final String defaultTimezone) {
        JSONObject event = new JSONObject();
        if (present(draft.title())) {
            event.put("subject", draft.title());
        }
        if (present(draft.description())) {
            event.put("body", new JSONObject().put("contentType", "text").put("content", draft.description()));
        }
        if (present(draft.location())) {
            event.put("location", new JSONObject().put("displayName", draft.location()));
        }

        String timezone = draft.timezone() != null ? draft.timezone() : defaultTimezone;
        boolean allDay = draft.isAllDay() || isDateOnly(draft.start());
        if (allDay) {
            event.put("isAllDay", true);
        }
        if (draft.start() != null) {
            event.put("start", timePoint(draft.start(), allDay, timezone));
        }
        if (draft.end() != null || draft.start() != null) {
            String end = draft.end() != null ? draft.end() : draft.start();
            event.put("end", timePoint(end, allDay, timezone));
        }

        if (!draft.attendees().isEmpty()) {
            JSONArray attendees = new JSONArray();
            for (String email : draft.attendees()) {
                attendees.put(new JSONObject()
                        .put("emailAddress", new JSONObject().put("address", email))
                        .put("type", "required"));
            }
            event.put("attendees", attendees);
        }

        if (draft.reminderMinutes() != null) {
            event.put("isReminderOn", true);
            event.put("reminderMinutesBeforeStart", draft.reminderMinutes());
        }
        return event;
    }

    public static CalendarEvent toCalendarEvent(final JSONObject event) {
        boolean allDay = event.optBoolean("isAllDay", false);
        return new CalendarEvent(
                event.optString("id", null),
                event.has("subject") ? event.optString("subject", null) : null,
                readTimePoint(event.optJSONObject("start")),
                readTimePoint(event.optJSONObject("end")),
                allDay,
                readLocation(event.optJSONObject("location")),
                readAttendees(event.optJSONArray("attendees")),
                readBody(event.optJSONObject("body")),
                null,
                event.has("webLink") ? event.optString("webLink", null) : null);
    }

    private static JSONObject timePoint(final String iso, final boolean allDay, final String timezone) {
        String dateTime = allDay ? datePart(iso) + "T00:00:00" : withSeconds(iso);
        return new JSONObject().put("dateTime", dateTime).put("timeZone", timezone);
    }

    private static String readTimePoint(final JSONObject point) {
        return point == null ? null : point.optString("dateTime", null);
    }

    private static String readLocation(final JSONObject location) {
        if (location == null) {
            return null;
        }
        String name = location.optString("displayName", "");
        return name.isEmpty() ? null : name;
    }

    private static String readBody(final JSONObject body) {
        if (body == null) {
            return null;
        }
        String content = body.optString("content", "");
        return content.isEmpty() ? null : content;
    }

    private static List<String> readAttendees(final JSONArray array) {
        List<String> out = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject email = array.getJSONObject(i).optJSONObject("emailAddress");
                String address = email == null ? "" : email.optString("address", "");
                if (!address.isEmpty()) {
                    out.add(address);
                }
            }
        }
        return out;
    }

    private static boolean isDateOnly(final String iso) {
        return iso != null && !iso.contains("T");
    }

    private static String datePart(final String iso) {
        int t = iso.indexOf('T');
        return t > 0 ? iso.substring(0, t) : iso;
    }

    private static String withSeconds(final String iso) {
        int t = iso.indexOf('T');
        if (t < 0) {
            return iso;
        }
        String time = iso.substring(t + 1);
        return time.length() == 5 ? iso + ":00" : iso;
    }

    private static boolean present(final String s) {
        return s != null && !s.isBlank();
    }
}
