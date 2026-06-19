package com.tsp.jimi_api.services.calendar;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import com.tsp.jimi_api.services.calendar.local.LocalDbCalendarProvider;
import com.tsp.jimi_api.support.FakeCalendarProvider;
import com.tsp.jimi_api.support.InMemoryAgendaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarServiceTest {

    private final FakeCalendarProvider external = new FakeCalendarProvider();
    private final LocalDbCalendarProvider local = new LocalDbCalendarProvider(new InMemoryAgendaRepository());
    private final CalendarService service = new CalendarService(List.of(external, local), local);

    private CalendarEvent event(final String id, final String title, final String start) {
        return new CalendarEvent(id, title, start, null, false, null, List.of(), null, null, "url/" + id);
    }

    @Test
    void hasExternalConnectedReflectsExternalProviderState() {
        external.connected = true;
        assertThat(service.hasExternalConnected("u1")).isTrue();
        external.connected = false;
        assertThat(service.hasExternalConnected("u1")).isFalse();
    }

    @Test
    void resolveForLegacyAlwaysReturnsTheLocalProvider() {
        assertThat(service.resolveFor("u1", false)).isSameAs(local);
    }

    @Test
    void resolveExternalThrowsWhenNothingConnected() {
        external.connected = false;
        assertThatThrownBy(() -> service.resolveExternal("u1"))
                .isInstanceOf(CalendarNotConnectedException.class);
    }

    @Test
    void emptyMatcherMatchesNothing_soAnUnderspecifiedRequestNeverSweepsTheCalendar() {
        external.events = List.of(event("1", "Dentist", "2026-06-20T09:00"));

        assertThat(service.matchEvents(external, "u1", EventDraft.empty())).isEmpty();
    }

    @Test
    void matchesByTitleSubstringCaseInsensitive() {
        external.events = List.of(
                event("1", "Dentist appointment", "2026-06-20T09:00"),
                event("2", "Team standup", "2026-06-20T10:00"));

        List<CalendarEvent> matches = service.matchEvents(external, "u1",
                new EventDraft("dentist", null, null, null, null, null, List.of(), null, null, null, null));

        assertThat(matches).extracting(CalendarEvent::id).containsExactly("1");
    }

    @Test
    void matchesByDateAndReturnsSeveralWhenAmbiguous() {
        external.events = List.of(
                event("1", "Call", "2026-06-20T09:00"),
                event("2", "Lunch", "2026-06-20T12:00"),
                event("3", "Gym", "2026-06-21T18:00"));

        List<CalendarEvent> matches = service.matchEvents(external, "u1",
                new EventDraft(null, "2026-06-20", null, null, null, null, List.of(), null, null, null, null));

        assertThat(matches).extracting(CalendarEvent::id).containsExactly("1", "2");
    }
}
