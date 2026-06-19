package com.tsp.jimi_api.records;

import com.tsp.jimi_api.enums.Type;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider-neutral <em>write</em> model: the structured intent extracted from
 * the user's message, used to create or edit a calendar event.
 *
 * <p>Every field is optional so a partial draft can be built across several
 * conversation turns. For a CREATE, {@link #title()} and {@link #start()} are
 * the only hard requirements (see {@code ConversationService}); everything
 * else is passed through to the calendar when present.
 *
 * <p>This covers the native options offered by real calendars: location,
 * attendees, description, all-day, recurrence (RRULE), reminders, timezone and
 * a PRO/PERSONAL hint mapped to a calendar colour where the provider supports
 * it.
 *
 * @param title          event summary/title
 * @param start          ISO-8601 start ("2026-06-20T14:00" or "2026-06-20" for all-day)
 * @param end            ISO-8601 end (defaulted by the provider when null on a timed event)
 * @param allDay         true to create an all-day event
 * @param timezone       IANA timezone id (e.g. "Europe/Paris"); provider/user default when null
 * @param location       free-text location
 * @param attendees      attendee email addresses
 * @param description    event notes/body
 * @param recurrence     iCal RRULE (e.g. "RRULE:FREQ=WEEKLY;BYDAY=MO")
 * @param reminderMinutes popup reminder, minutes before start
 * @param type           PRO / PERSONAL hint (optional, mapped to colour where supported)
 */
public record EventDraft(
        String title,
        String start,
        String end,
        Boolean allDay,
        String timezone,
        String location,
        List<String> attendees,
        String description,
        String recurrence,
        Integer reminderMinutes,
        Type type) {

    public EventDraft {
        attendees = attendees == null ? List.of() : List.copyOf(attendees);
    }

    public static EventDraft empty() {
        return new EventDraft(null, null, null, null, null, null, List.of(), null, null, null, null);
    }

    public boolean isAllDay() {
        return Boolean.TRUE.equals(allDay);
    }

    public boolean isEmpty() {
        return title == null && start == null && end == null && allDay == null
                && timezone == null && location == null && attendees.isEmpty()
                && description == null && recurrence == null && reminderMinutes == null && type == null;
    }

    /**
     * Parses one event-field block ({@code new_value} / {@code old_value}) from
     * the LLM's JSON. Unknown or malformed fields are simply dropped — never
     * guessed — so a sloppy model reply can't fabricate event details.
     */
    public static EventDraft parse(final JSONObject json) {
        if (json == null || json.length() == 0) {
            return empty();
        }
        return new EventDraft(
                optString(json, "title"),
                optString(json, "start"),
                optString(json, "end"),
                json.has("all_day") ? json.optBoolean("all_day") : null,
                optString(json, "timezone"),
                optString(json, "location"),
                parseAttendees(json.optJSONArray("attendees")),
                optString(json, "description"),
                optString(json, "recurrence"),
                json.has("reminder_minutes") && !json.isNull("reminder_minutes")
                        ? json.optInt("reminder_minutes") : null,
                parseType(optString(json, "type")));
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        putIfPresent(json, "title", title);
        putIfPresent(json, "start", start);
        putIfPresent(json, "end", end);
        if (allDay != null) {
            json.put("all_day", allDay);
        }
        putIfPresent(json, "timezone", timezone);
        putIfPresent(json, "location", location);
        if (!attendees.isEmpty()) {
            json.put("attendees", new JSONArray(attendees));
        }
        putIfPresent(json, "description", description);
        putIfPresent(json, "recurrence", recurrence);
        if (reminderMinutes != null) {
            json.put("reminder_minutes", reminderMinutes);
        }
        if (type != null) {
            json.put("type", type.name());
        }
        return json;
    }

    private static String optString(final JSONObject json, final String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        String value = json.optString(key, "").trim();
        return value.isEmpty() ? null : value;
    }

    private static List<String> parseAttendees(final JSONArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String email = array.optString(i, "").trim();
            if (!email.isEmpty()) {
                out.add(email);
            }
        }
        return out;
    }

    private static Type parseType(final String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Type.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void putIfPresent(final JSONObject json, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            json.put(key, value);
        }
    }
}
