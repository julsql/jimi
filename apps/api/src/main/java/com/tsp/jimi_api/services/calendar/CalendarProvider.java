package com.tsp.jimi_api.services.calendar;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;

import java.util.List;

/**
 * Provider-agnostic gateway to a user's real calendar.
 *
 * <p>JIMI talks to the calendar the user already lives in (Google primary,
 * plus CalDAV and Microsoft) instead of keeping its own copy. Each concrete
 * implementation wraps one provider's API and is registered as a Spring bean;
 * {@link CalendarService} picks the one the current user has connected.
 *
 * <p>Mirrors the {@code LlmClient} pattern: add a provider by implementing this
 * interface and registering the bean — no changes to {@code ChatService}.
 *
 * <p><strong>Safety contract:</strong> implementations perform exactly the
 * mutation requested and never delete or edit anything that was not explicitly
 * targeted. Destructive operations ({@link #update}, {@link #delete}) are only
 * ever called by the server after the user has confirmed (see
 * {@code ChatService} confirmation flow) — never directly from an LLM turn.
 */
public interface CalendarProvider {

    /** Stable id of this provider: "google", "caldav", "microsoft". */
    String id();

    /** True when {@code userId} has a usable (linked, non-expired) account here. */
    boolean isConnected(String userId);

    /**
     * Lists events between two ISO-8601 instants, oldest first. Used both for
     * GET answers and to resolve which event(s) an edit/delete refers to.
     */
    List<CalendarEvent> findEvents(String userId, String fromIso, String toIso);

    /** Creates a new event and returns it (with provider id + deep link populated). */
    CalendarEvent create(String userId, EventDraft draft);

    /**
     * Applies the non-null fields of {@code changes} to an existing event.
     * Called only after explicit user confirmation.
     */
    CalendarEvent update(String userId, String eventId, EventDraft changes);

    /** Permanently removes an event. Called only after explicit user confirmation. */
    void delete(String userId, String eventId);
}
