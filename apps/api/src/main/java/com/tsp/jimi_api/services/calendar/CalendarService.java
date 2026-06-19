package com.tsp.jimi_api.services.calendar;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

/**
 * Routes calendar operations to whichever provider the user has connected.
 *
 * <p>Spring injects every {@link CalendarProvider} bean; {@link #resolve} picks
 * the first one the user is linked to. With no provider connected (or none
 * registered yet) {@link #isAnyConnected} returns false and callers surface a
 * NEEDS_CONNECTION response rather than failing.
 *
 * <p>For edit/delete, {@link #matchEvents} narrows the user's calendar down to
 * the event(s) a natural-language request refers to. The match is deliberately
 * conservative: a request with no usable matcher returns nothing, so the LLM
 * can never trigger a blind "delete everything".
 */
@Service
public class CalendarService {

    /** How far back/forward we look when resolving an edit/delete or a GET. */
    private static final int LOOKBACK_DAYS = 31;
    private static final int LOOKAHEAD_DAYS = 366;

    private final List<CalendarProvider> providers;

    public CalendarService(final List<CalendarProvider> providers) {
        this.providers = providers;
    }

    public boolean isAnyConnected(final String userId) {
        return providers.stream().anyMatch(p -> p.isConnected(userId));
    }

    public CalendarProvider resolve(final String userId) {
        return providers.stream()
                .filter(p -> p.isConnected(userId))
                .findFirst()
                .orElseThrow(() -> new CalendarNotConnectedException(userId));
    }

    public CalendarEvent create(final String userId, final EventDraft draft) {
        return resolve(userId).create(userId, draft);
    }

    public CalendarEvent update(final String userId, final String eventId, final EventDraft changes) {
        return resolve(userId).update(userId, eventId, changes);
    }

    public void delete(final String userId, final String eventId) {
        resolve(userId).delete(userId, eventId);
    }

    /** Events in the default look-around window, used to answer GET questions. */
    public List<CalendarEvent> upcomingEvents(final String userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return resolve(userId).findEvents(
                userId,
                now.minusDays(LOOKBACK_DAYS).toString(),
                now.plusDays(LOOKAHEAD_DAYS).toString());
    }

    /**
     * Resolves which existing event(s) a natural-language edit/delete refers to.
     *
     * <p>A matcher must carry at least a title or a date — an empty matcher
     * returns no events on purpose, so an under-specified request never
     * sweeps the whole calendar.
     *
     * @param matcher the {@code old_value} block the LLM extracted
     * @return matching events (possibly empty, possibly several → caller asks
     *         the user to disambiguate)
     */
    public List<CalendarEvent> matchEvents(final String userId, final EventDraft matcher) {
        if (matcher == null || (isBlank(matcher.title()) && isBlank(matcher.start()))) {
            return List.of();
        }
        String titleNeedle = matcher.title() == null ? null : matcher.title().toLowerCase(Locale.ROOT).trim();
        String dateNeedle = datePart(matcher.start());

        return upcomingEvents(userId).stream()
                .filter(e -> titleNeedle == null
                        || (e.title() != null && e.title().toLowerCase(Locale.ROOT).contains(titleNeedle)))
                .filter(e -> dateNeedle == null
                        || (e.start() != null && e.start().startsWith(dateNeedle)))
                .toList();
    }

    private static String datePart(final String iso) {
        if (isBlank(iso)) {
            return null;
        }
        int t = iso.indexOf('T');
        return t > 0 ? iso.substring(0, t) : iso;
    }

    private static boolean isBlank(final String s) {
        return s == null || s.isBlank();
    }
}
