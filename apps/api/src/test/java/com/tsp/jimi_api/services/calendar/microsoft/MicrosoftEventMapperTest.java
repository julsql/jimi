package com.tsp.jimi_api.services.calendar.microsoft;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MicrosoftEventMapperTest {

    @Test
    void mapsATimedEventWithNativeFields() throws Exception {
        EventDraft draft = new EventDraft(
                "Lunch with Alex", "2026-06-20T13:00", "2026-06-20T14:00", false,
                "Europe/Paris", "Café X", List.of("alex@example.com"), "catch-up",
                null, 30, null);

        JSONObject g = MicrosoftEventMapper.toGraphEvent(draft, "UTC");

        assertThat(g.getString("subject")).isEqualTo("Lunch with Alex");
        assertThat(g.getJSONObject("location").getString("displayName")).isEqualTo("Café X");
        assertThat(g.getJSONObject("body").getString("content")).isEqualTo("catch-up");
        assertThat(g.getJSONObject("start").getString("dateTime")).isEqualTo("2026-06-20T13:00:00");
        assertThat(g.getJSONObject("start").getString("timeZone")).isEqualTo("Europe/Paris");
        assertThat(g.getJSONObject("end").getString("dateTime")).isEqualTo("2026-06-20T14:00:00");
        assertThat(g.getJSONArray("attendees").getJSONObject(0)
                .getJSONObject("emailAddress").getString("address")).isEqualTo("alex@example.com");
        assertThat(g.getBoolean("isReminderOn")).isTrue();
        assertThat(g.getInt("reminderMinutesBeforeStart")).isEqualTo(30);
    }

    @Test
    void mapsAnAllDayEvent() throws Exception {
        EventDraft draft = new EventDraft("Holiday", "2026-07-01", null, true,
                null, null, List.of(), null, null, null, null);

        JSONObject g = MicrosoftEventMapper.toGraphEvent(draft, "UTC");

        assertThat(g.getBoolean("isAllDay")).isTrue();
        assertThat(g.getJSONObject("start").getString("dateTime")).isEqualTo("2026-07-01T00:00:00");
    }

    @Test
    void readsAGraphEventBackIntoTheNeutralModel() throws Exception {
        JSONObject g = new JSONObject("""
                {
                  "id": "AAMk1",
                  "subject": "Standup",
                  "isAllDay": false,
                  "webLink": "https://outlook.office365.com/evt/AAMk1",
                  "location": {"displayName": "Teams"},
                  "body": {"contentType": "text", "content": "daily"},
                  "start": {"dateTime": "2026-06-20T09:00:00.0000000", "timeZone": "Europe/Paris"},
                  "end": {"dateTime": "2026-06-20T09:15:00.0000000", "timeZone": "Europe/Paris"},
                  "attendees": [
                    {"emailAddress": {"address": "a@x.com"}},
                    {"emailAddress": {"address": "b@x.com"}}
                  ]
                }
                """);

        CalendarEvent e = MicrosoftEventMapper.toCalendarEvent(g);

        assertThat(e.id()).isEqualTo("AAMk1");
        assertThat(e.title()).isEqualTo("Standup");
        assertThat(e.start()).isEqualTo("2026-06-20T09:00:00.0000000");
        assertThat(e.allDay()).isFalse();
        assertThat(e.location()).isEqualTo("Teams");
        assertThat(e.description()).isEqualTo("daily");
        assertThat(e.attendees()).containsExactly("a@x.com", "b@x.com");
        assertThat(e.url()).isEqualTo("https://outlook.office365.com/evt/AAMk1");
    }
}
