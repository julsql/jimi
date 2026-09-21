package com.tsp.jimi_api.services.calendar;

import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import com.tsp.jimi_api.services.calendar.local.LocalDbCalendarProvider;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Picks the calendar provider for a request and helps narrow events for
 * edit/delete.
 *
 * <p>Two worlds coexist:
 * <ul>
 *   <li><b>legacy</b> ({@code calendarMode=false}) → the {@link LocalDbCalendarProvider}
 *       (JIMI's own {@code agenda} table). Always available; this is what
 *       already-deployed apps get, unchanged.</li>
 *   <li><b>calendar</b> ({@code calendarMode=true}) → the external provider the
 *       user has connected (Google / CalDAV / Microsoft), or
 *       {@link #hasExternalConnected} is false and the caller returns
 *       NEEDS_CONNECTION.</li>
 * </ul>
 *
 * <p>{@link #matchEvents} narrows to the event(s) a natural-language edit/delete
 * refers to. An empty matcher returns nothing, so an under-specified request
 * never sweeps the whole calendar.
 */
@Service
public class CalendarService {

    private static final int LOOKBACK_DAYS = 31;
    private static final int LOOKAHEAD_DAYS = 366;

    private final List<CalendarProvider> providers;
    private final LocalDbCalendarProvider localProvider;

    public CalendarService(final List<CalendarProvider> providers,
                           final LocalDbCalendarProvider localProvider) {
        this.providers = providers;
        this.localProvider = localProvider;
    }

    /** The provider to use for a request, given whether it opted into calendar mode. */
    public CalendarProvider resolveFor(final String userId, final boolean calendarMode) {
        return calendarMode ? resolveExternal(userId) : localProvider;
    }

    public boolean hasExternalConnected(final String userId) {
        return externalProviders().anyMatch(p -> p.isConnected(userId));
    }

    public CalendarProvider resolveExternal(final String userId) {
        return externalProviders()
                .filter(p -> p.isConnected(userId))
                .findFirst()
                .orElseThrow(() -> new CalendarNotConnectedException(userId));
    }

    public LocalDbCalendarProvider local() {
        return localProvider;
    }

    private Stream<CalendarProvider> externalProviders() {
        return providers.stream().filter(p -> !LocalDbCalendarProvider.ID.equals(p.id()));
    }

    /** Events in the default look-around window, used to answer GET questions. */
    public List<CalendarEvent> upcomingEvents(final CalendarProvider provider, final String userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return provider.findEvents(
                userId,
                now.minusDays(LOOKBACK_DAYS).toString(),
                now.plusDays(LOOKAHEAD_DAYS).toString());
    }

    /**
     * Resolves which existing event(s) a natural-language edit/delete refers to.
     * An empty matcher (no title and no date) returns nothing on purpose.
     */
    public List<CalendarEvent> matchEvents(final CalendarProvider provider,
                                           final String userId, final EventDraft matcher) {
        if (matcher == null || (isBlank(matcher.title()) && isBlank(matcher.start()))) {
            return List.of();
        }
        String titleNeedle = matcher.title() == null ? null : matcher.title().toLowerCase(Locale.ROOT).trim();
        String dateNeedle = datePart(matcher.start());

        return upcomingEvents(provider, userId).stream()
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
