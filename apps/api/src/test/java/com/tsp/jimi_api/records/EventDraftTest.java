package com.tsp.jimi_api.records;

import com.tsp.jimi_api.enums.Type;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventDraftTest {

    @Test
    void parsesTheFullSetOfNativeFields() throws Exception {
        JSONObject json = new JSONObject("""
                {
                  "title": "Lunch with Alex",
                  "start": "2026-06-20T13:00",
                  "end": "2026-06-20T14:00",
                  "all_day": false,
                  "timezone": "Europe/Paris",
                  "location": "Café X",
                  "attendees": ["alex@example.com", "bob@example.com"],
                  "description": "monthly catch-up",
                  "recurrence": "RRULE:FREQ=MONTHLY",
                  "reminder_minutes": 30,
                  "type": "PERSONAL"
                }
                """);

        EventDraft draft = EventDraft.parse(json);

        assertThat(draft.title()).isEqualTo("Lunch with Alex");
        assertThat(draft.start()).isEqualTo("2026-06-20T13:00");
        assertThat(draft.end()).isEqualTo("2026-06-20T14:00");
        assertThat(draft.isAllDay()).isFalse();
        assertThat(draft.timezone()).isEqualTo("Europe/Paris");
        assertThat(draft.location()).isEqualTo("Café X");
        assertThat(draft.attendees()).containsExactly("alex@example.com", "bob@example.com");
        assertThat(draft.description()).isEqualTo("monthly catch-up");
        assertThat(draft.recurrence()).isEqualTo("RRULE:FREQ=MONTHLY");
        assertThat(draft.reminderMinutes()).isEqualTo(30);
        assertThat(draft.type()).isEqualTo(Type.PERSONAL);
    }

    @Test
    void dropsBlankAndUnknownFieldsRatherThanGuessing() throws Exception {
        EventDraft draft = EventDraft.parse(new JSONObject("""
                { "title": "  ", "start": "2026-06-20", "type": "NONSENSE" }
                """));

        assertThat(draft.title()).isNull();
        assertThat(draft.start()).isEqualTo("2026-06-20");
        assertThat(draft.type()).isNull();
        assertThat(draft.attendees()).isEmpty();
    }

    @Test
    void emptyDraftIsEmpty() throws Exception {
        assertThat(EventDraft.parse(new JSONObject())).matches(EventDraft::isEmpty);
        assertThat(EventDraft.empty().isEmpty()).isTrue();
    }

    @Test
    void toJsonRoundTripsPresentFieldsOnly() throws Exception {
        EventDraft draft = EventDraft.parse(new JSONObject("""
                { "title": "Standup", "start": "2026-06-20T09:00", "reminder_minutes": 5 }
                """));

        JSONObject json = draft.toJson();

        assertThat(json.length()).isEqualTo(3);
        assertThat(json.has("title")).isTrue();
        assertThat(json.has("start")).isTrue();
        assertThat(json.has("reminder_minutes")).isTrue();
        assertThat(json.getString("title")).isEqualTo("Standup");
        assertThat(json.getInt("reminder_minutes")).isEqualTo(5);
    }
}
