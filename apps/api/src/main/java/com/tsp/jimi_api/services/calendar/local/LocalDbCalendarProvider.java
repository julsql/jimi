package com.tsp.jimi_api.services.calendar.local;

import com.tsp.jimi_api.entities.Agenda;
import com.tsp.jimi_api.enums.Type;
import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import com.tsp.jimi_api.repositories.AgendaRepository;
import com.tsp.jimi_api.services.calendar.CalendarProvider;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * The legacy "calendar": JIMI's own {@code agenda} table. This is the default
 * provider used when a request does not opt into calendar mode
 * ({@code calendarMode=false}), preserving the pre-pivot behaviour for already
 * deployed apps — events live in the DB, {@code GET /agenda} lists them, and
 * edit/delete happen directly (no external account, no confirmation step).
 *
 * <p>It maps the rich provider-neutral {@link EventDraft}/{@link CalendarEvent}
 * onto the old flat schema (date + begin/end time + type + title); fields the
 * table can't hold (location, attendees, recurrence, reminder) are dropped.
 *
 * <p>Always "connected" — it needs no linking.
 */
@Component
public class LocalDbCalendarProvider implements CalendarProvider {

    public static final String ID = "local";

    private final AgendaRepository repository;

    public LocalDbCalendarProvider(final AgendaRepository repository) {
        this.repository = repository;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isConnected(final String userId) {
        return true;
    }

    @Override
    public List<CalendarEvent> findEvents(final String userId, final String fromIso, final String toIso) {
        List<CalendarEvent> events = new ArrayList<>();
        for (Agenda a : repository.findByUserId(userId)) {
            events.add(toCalendarEvent(a));
        }
        return events;
    }

    @Override
    public CalendarEvent create(final String userId, final EventDraft draft) {
        Agenda saved = repository.save(new Agenda(
                date(draft.start()),
                time(draft.start()),
                time(draft.end()),
                draft.type() != null ? draft.type() : Type.UNDEFINED,
                draft.title(),
                userId));
        return toCalendarEvent(saved);
    }

    @Override
    public CalendarEvent update(final String userId, final String eventId, final EventDraft changes) {
        Agenda event = repository.findById(parseId(eventId))
                .filter(a -> userId.equals(a.getUserId()))
                .orElseThrow(() -> new IllegalStateException("No such event: " + eventId));
        if (changes.title() != null) {
            event.setTitle(changes.title());
        }
        if (changes.type() != null) {
            event.setType(changes.type());
        }
        if (changes.start() != null) {
            event.setDate(date(changes.start()));
            Time begin = time(changes.start());
            if (begin != null) {
                event.setBegin(begin);
            }
        }
        if (changes.end() != null) {
            event.setEnd(time(changes.end()));
        }
        return toCalendarEvent(repository.save(event));
    }

    @Override
    public void delete(final String userId, final String eventId) {
        repository.findById(parseId(eventId))
                .filter(a -> userId.equals(a.getUserId()))
                .ifPresent(repository::delete);
    }

    /** Whole-user listing, used by GET /agenda (sorted by date then start). */
    public List<Agenda> listRaw(final String userId) {
        return StreamSupport.stream(repository.findByUserId(userId).spliterator(), false).toList();
    }

    private CalendarEvent toCalendarEvent(final Agenda a) {
        boolean allDay = a.getBegin() == null;
        String start = a.getDate() == null ? null
                : a.getDate().toString() + (allDay ? "" : "T" + hhmm(a.getBegin()));
        String end = a.getEnd() == null || a.getDate() == null ? null
                : a.getDate().toString() + "T" + hhmm(a.getEnd());
        return new CalendarEvent(
                String.valueOf(a.getId()),
                a.getTitle(),
                start,
                end,
                allDay,
                null,
                List.of(),
                a.getType() == null ? null : a.getType().name(),
                null,
                null);
    }

    private static Date date(final String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        int t = iso.indexOf('T');
        String datePart = t > 0 ? iso.substring(0, t) : iso;
        try {
            return Date.valueOf(datePart);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Time time(final String iso) {
        if (iso == null) {
            return null;
        }
        int t = iso.indexOf('T');
        if (t < 0) {
            return null; // date-only / all-day
        }
        String timePart = iso.substring(t + 1);
        if (timePart.length() == 5) {
            timePart = timePart + ":00";
        } else if (timePart.length() < 5) {
            return null;
        } else if (timePart.length() > 8) {
            timePart = timePart.substring(0, 8);
        }
        try {
            return Time.valueOf(timePart);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String hhmm(final Time time) {
        return time.toString().substring(0, 5);
    }

    private static Long parseId(final String eventId) {
        try {
            return Long.parseLong(eventId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid local event id: " + eventId);
        }
    }
}
