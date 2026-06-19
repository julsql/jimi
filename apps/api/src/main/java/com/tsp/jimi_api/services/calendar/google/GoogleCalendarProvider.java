package com.tsp.jimi_api.services.calendar.google;

import com.tsp.jimi_api.configurations.GoogleOAuthProperties;
import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.EventDraft;
import com.tsp.jimi_api.services.CalendarAccountService;
import com.tsp.jimi_api.services.calendar.CalendarProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link CalendarProvider} backed by the user's Google Calendar.
 *
 * <p>Connected only when OAuth is configured AND the user has linked a Google
 * account — otherwise {@link #isConnected} returns false and JIMI falls back to
 * NEEDS_CONNECTION rather than erroring.
 */
@Component
public class GoogleCalendarProvider implements CalendarProvider {

    private final CalendarAccountService accounts;
    private final GoogleApiClient api;
    private final GoogleOAuthProperties props;
    private final String defaultTimezone = ZoneId.systemDefault().getId();

    public GoogleCalendarProvider(final CalendarAccountService accounts,
                                  final GoogleApiClient api,
                                  final GoogleOAuthProperties props) {
        this.accounts = accounts;
        this.api = api;
        this.props = props;
    }

    @Override
    public String id() {
        return CalendarAccountService.GOOGLE;
    }

    @Override
    public boolean isConnected(final String userId) {
        return props.isConfigured() && accounts.isConnected(userId, id());
    }

    @Override
    public List<CalendarEvent> findEvents(final String userId, final String fromIso, final String toIso) {
        JSONObject response = api.listEvents(userId, fromIso, toIso);
        List<CalendarEvent> events = new ArrayList<>();
        JSONArray items = response.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                events.add(GoogleEventMapper.toCalendarEvent(items.getJSONObject(i)));
            }
        }
        return events;
    }

    @Override
    public CalendarEvent create(final String userId, final EventDraft draft) {
        JSONObject created = api.insertEvent(userId,
                GoogleEventMapper.toGoogleEvent(draft, defaultTimezone));
        return GoogleEventMapper.toCalendarEvent(created);
    }

    @Override
    public CalendarEvent update(final String userId, final String eventId, final EventDraft changes) {
        JSONObject patched = api.patchEvent(userId, eventId,
                GoogleEventMapper.toGoogleEvent(changes, defaultTimezone));
        return GoogleEventMapper.toCalendarEvent(patched);
    }

    @Override
    public void delete(final String userId, final String eventId) {
        api.deleteEvent(userId, eventId);
    }
}
