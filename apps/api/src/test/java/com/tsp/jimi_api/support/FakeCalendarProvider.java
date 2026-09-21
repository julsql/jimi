package com.tsp.jimi_api.support;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import com.tsp.jimi_api.services.calendar.CalendarProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory {@link CalendarProvider} for tests. Records every mutation so a
 * test can assert that nothing was written/deleted before confirmation.
 */
public class FakeCalendarProvider implements CalendarProvider {

    public boolean connected = true;
    public List<CalendarEvent> events = new ArrayList<>();

    public final List<EventDraft> created = new ArrayList<>();
    public final List<String> updated = new ArrayList<>();
    public final List<String> deleted = new ArrayList<>();

    @Override
    public String id() {
        return "fake";
    }

    @Override
    public boolean isConnected(final String userId) {
        return connected;
    }

    @Override
    public List<CalendarEvent> findEvents(final String userId, final String fromIso, final String toIso) {
        return events;
    }

    @Override
    public CalendarEvent create(final String userId, final EventDraft draft) {
        created.add(draft);
        return new CalendarEvent("new-1", draft.title(), draft.start(), draft.end(),
                draft.isAllDay(), draft.location(), draft.attendees(), draft.description(),
                draft.recurrence(), "https://calendar/new-1");
    }

    @Override
    public CalendarEvent update(final String userId, final String eventId, final EventDraft changes) {
        updated.add(eventId);
        return new CalendarEvent(eventId, changes.title(), changes.start(), changes.end(),
                changes.isAllDay(), changes.location(), changes.attendees(), changes.description(),
                changes.recurrence(), "https://calendar/" + eventId);
    }

    @Override
    public void delete(final String userId, final String eventId) {
        deleted.add(eventId);
    }
}
