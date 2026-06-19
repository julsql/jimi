package com.tsp.jimi_api.services.calendar;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import com.tsp.jimi_api.support.FakeCalendarProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarServiceTest {

    private final FakeCalendarProvider provider = new FakeCalendarProvider();
    private final CalendarService service = new CalendarService(List.of(provider));

    private CalendarEvent event(final String id, final String title, final String start) {
        return new CalendarEvent(id, title, start, null, false, null, List.of(), null, null, "url/" + id);
    }

    @Test
    void isAnyConnectedReflectsProviderState() {
        provider.connected = true;
        assertThat(service.isAnyConnected("u1")).isTrue();
        provider.connected = false;
        assertThat(service.isAnyConnected("u1")).isFalse();
    }

    @Test
    void resolveThrowsWhenNothingConnected() {
        provider.connected = false;
        assertThatThrownBy(() -> service.resolve("u1"))
                .isInstanceOf(CalendarNotConnectedException.class);
    }

    @Test
    void emptyMatcherMatchesNothing_soAnUnderspecifiedRequestNeverSweepsTheCalendar() {
        provider.events = List.of(event("1", "Dentist", "2026-06-20T09:00"));

        assertThat(service.matchEvents("u1", EventDraft.empty())).isEmpty();
    }

    @Test
    void matchesByTitleSubstringCaseInsensitive() {
        provider.events = List.of(
                event("1", "Dentist appointment", "2026-06-20T09:00"),
                event("2", "Team standup", "2026-06-20T10:00"));

        List<CalendarEvent> matches = service.matchEvents("u1",
                new EventDraft("dentist", null, null, null, null, null, List.of(), null, null, null, null));

        assertThat(matches).extracting(CalendarEvent::id).containsExactly("1");
    }

    @Test
    void matchesByDateAndReturnsSeveralWhenAmbiguous() {
        provider.events = List.of(
                event("1", "Call", "2026-06-20T09:00"),
                event("2", "Lunch", "2026-06-20T12:00"),
                event("3", "Gym", "2026-06-21T18:00"));

        List<CalendarEvent> matches = service.matchEvents("u1",
                new EventDraft(null, "2026-06-20", null, null, null, null, List.of(), null, null, null, null));

        assertThat(matches).extracting(CalendarEvent::id).containsExactly("1", "2");
    }
}
