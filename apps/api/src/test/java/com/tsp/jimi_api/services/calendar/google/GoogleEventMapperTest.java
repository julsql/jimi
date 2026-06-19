package com.tsp.jimi_api.services.calendar.google;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleEventMapperTest {

    @Test
    void mapsATimedEventWithAllNativeFields() throws Exception {
        EventDraft draft = new EventDraft(
                "Lunch with Alex", "2026-06-20T13:00", "2026-06-20T14:00", false,
                "Europe/Paris", "Café X", List.of("alex@example.com"), "catch-up",
                "RRULE:FREQ=WEEKLY;BYDAY=MO", 30, null);

        JSONObject g = GoogleEventMapper.toGoogleEvent(draft, "UTC");

        assertThat(g.getString("summary")).isEqualTo("Lunch with Alex");
        assertThat(g.getString("location")).isEqualTo("Café X");
        assertThat(g.getString("description")).isEqualTo("catch-up");
        assertThat(g.getJSONObject("start").getString("dateTime")).isEqualTo("2026-06-20T13:00:00");
        assertThat(g.getJSONObject("start").getString("timeZone")).isEqualTo("Europe/Paris");
        assertThat(g.getJSONArray("attendees").getJSONObject(0).getString("email"))
                .isEqualTo("alex@example.com");
        assertThat(g.getJSONArray("recurrence").getString(0)).isEqualTo("RRULE:FREQ=WEEKLY;BYDAY=MO");
        assertThat(g.getJSONObject("reminders").getBoolean("useDefault")).isFalse();
        assertThat(g.getJSONObject("reminders").getJSONArray("overrides").getJSONObject(0).getInt("minutes"))
                .isEqualTo(30);
    }

    @Test
    void mapsAnAllDayEventToADateField() throws Exception {
        EventDraft draft = new EventDraft("Holiday", "2026-07-01", null, true,
                null, null, List.of(), null, null, null, null);

        JSONObject g = GoogleEventMapper.toGoogleEvent(draft, "UTC");

        assertThat(g.getJSONObject("start").getString("date")).isEqualTo("2026-07-01");
        assertThat(g.getJSONObject("start").has("dateTime")).isFalse();
    }

    @Test
    void readsAGoogleEventBackIntoTheNeutralModel() throws Exception {
        JSONObject g = new JSONObject("""
                {
                  "id": "evt-1",
                  "summary": "Standup",
                  "htmlLink": "https://calendar.google.com/evt-1",
                  "location": "Zoom",
                  "description": "daily",
                  "start": {"dateTime": "2026-06-20T09:00:00+02:00"},
                  "end": {"dateTime": "2026-06-20T09:15:00+02:00"},
                  "attendees": [{"email": "a@x.com"}, {"email": "b@x.com"}],
                  "recurrence": ["RRULE:FREQ=DAILY"]
                }
                """);

        CalendarEvent e = GoogleEventMapper.toCalendarEvent(g);

        assertThat(e.id()).isEqualTo("evt-1");
        assertThat(e.title()).isEqualTo("Standup");
        assertThat(e.start()).isEqualTo("2026-06-20T09:00:00+02:00");
        assertThat(e.allDay()).isFalse();
        assertThat(e.location()).isEqualTo("Zoom");
        assertThat(e.attendees()).containsExactly("a@x.com", "b@x.com");
        assertThat(e.recurrence()).isEqualTo("RRULE:FREQ=DAILY");
        assertThat(e.url()).isEqualTo("https://calendar.google.com/evt-1");
    }

    @Test
    void detectsAllDayWhenGoogleReturnsADate() throws Exception {
        JSONObject g = new JSONObject("""
                { "id": "h1", "summary": "Off", "start": {"date": "2026-07-01"}, "end": {"date": "2026-07-02"} }
                """);

        assertThat(GoogleEventMapper.toCalendarEvent(g).allDay()).isTrue();
    }
}
