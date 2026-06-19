package com.tsp.jimi_api.services.calendar.caldav;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalDavICalMapperTest {

    @Test
    void buildsATimedVeventWithAllNativeFields() {
        EventDraft draft = new EventDraft(
                "Lunch with Alex", "2026-06-20T13:00", "2026-06-20T14:00", false,
                "Europe/Paris", "Café X", List.of("alex@example.com"), "catch-up",
                "RRULE:FREQ=WEEKLY;BYDAY=MO", 30, null);

        String ical = CalDavICalMapper.toICalendar(draft, "uid-1");

        assertThat(ical).contains("BEGIN:VCALENDAR").contains("BEGIN:VEVENT").contains("END:VCALENDAR");
        assertThat(ical).contains("UID:uid-1");
        assertThat(ical).contains("SUMMARY:Lunch with Alex");
        assertThat(ical).contains("DTSTART:20260620T130000");
        assertThat(ical).contains("DTEND:20260620T140000");
        assertThat(ical).contains("LOCATION:Café X");
        assertThat(ical).contains("DESCRIPTION:catch-up");
        assertThat(ical).contains("RRULE:FREQ=WEEKLY;BYDAY=MO");
        assertThat(ical).contains("ATTENDEE:mailto:alex@example.com");
    }

    @Test
    void buildsAnAllDayVeventWithValueDate() {
        EventDraft draft = new EventDraft("Holiday", "2026-07-01", null, true,
                null, null, List.of(), null, null, null, null);

        String ical = CalDavICalMapper.toICalendar(draft, "uid-2");

        assertThat(ical).contains("DTSTART;VALUE=DATE:20260701");
        assertThat(ical).doesNotContain("DTSTART:20260701T");
    }

    @Test
    void parsesATimedVeventBackIntoTheNeutralModel() {
        String ical = """
                BEGIN:VCALENDAR\r
                VERSION:2.0\r
                BEGIN:VEVENT\r
                UID:evt-1\r
                SUMMARY:Standup\r
                LOCATION:Zoom\r
                DESCRIPTION:daily\r
                DTSTART:20260620T090000\r
                DTEND:20260620T091500\r
                ATTENDEE:mailto:a@x.com\r
                ATTENDEE:mailto:b@x.com\r
                RRULE:FREQ=DAILY\r
                END:VEVENT\r
                END:VCALENDAR\r
                """;

        CalendarEvent e = CalDavICalMapper.toCalendarEvent(ical);

        assertThat(e.id()).isEqualTo("evt-1");
        assertThat(e.title()).isEqualTo("Standup");
        assertThat(e.start()).isEqualTo("2026-06-20T09:00:00");
        assertThat(e.end()).isEqualTo("2026-06-20T09:15:00");
        assertThat(e.allDay()).isFalse();
        assertThat(e.location()).isEqualTo("Zoom");
        assertThat(e.description()).isEqualTo("daily");
        assertThat(e.attendees()).containsExactly("a@x.com", "b@x.com");
        assertThat(e.recurrence()).isEqualTo("RRULE:FREQ=DAILY");
    }

    @Test
    void parsesAnAllDayVeventAsAllDayDateOnly() {
        String ical = """
                BEGIN:VEVENT\r
                UID:h1\r
                SUMMARY:Off\r
                DTSTART;VALUE=DATE:20260701\r
                DTEND;VALUE=DATE:20260702\r
                END:VEVENT\r
                """;

        CalendarEvent e = CalDavICalMapper.toCalendarEvent(ical);

        assertThat(e.allDay()).isTrue();
        assertThat(e.start()).isEqualTo("2026-07-01");
        assertThat(e.end()).isEqualTo("2026-07-02");
    }

    @Test
    void roundTripsADraftThroughICalendar() {
        EventDraft draft = new EventDraft("Sync", "2026-06-20T10:30", "2026-06-20T11:00",
                false, null, "HQ", List.of("c@x.com"), "weekly sync", null, null, null);

        CalendarEvent e = CalDavICalMapper.toCalendarEvent(CalDavICalMapper.toICalendar(draft, "rt-1"));

        assertThat(e.id()).isEqualTo("rt-1");
        assertThat(e.title()).isEqualTo("Sync");
        assertThat(e.start()).isEqualTo("2026-06-20T10:30:00");
        assertThat(e.end()).isEqualTo("2026-06-20T11:00:00");
        assertThat(e.location()).isEqualTo("HQ");
        assertThat(e.description()).isEqualTo("weekly sync");
        assertThat(e.attendees()).containsExactly("c@x.com");
    }

    @Test
    void applyChangesMergesNonNullFieldsAndKeepsUid() {
        String existing = CalDavICalMapper.toICalendar(new EventDraft(
                "Old title", "2026-06-20T13:00", "2026-06-20T14:00", false, null,
                "Old place", List.of("x@x.com"), "old notes", null, null, null), "keep-uid");

        EventDraft changes = new EventDraft("New title", null, null, null, null,
                null, List.of(), null, null, null, null);

        String updated = CalDavICalMapper.applyChanges(existing, changes);
        CalendarEvent e = CalDavICalMapper.toCalendarEvent(updated);

        assertThat(e.id()).isEqualTo("keep-uid");
        assertThat(e.title()).isEqualTo("New title");
        assertThat(e.start()).isEqualTo("2026-06-20T13:00:00");
        assertThat(e.location()).isEqualTo("Old place");
        assertThat(e.attendees()).containsExactly("x@x.com");
    }
}
