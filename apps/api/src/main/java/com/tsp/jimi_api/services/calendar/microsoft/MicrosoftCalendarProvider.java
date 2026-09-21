package com.tsp.jimi_api.services.calendar.microsoft;

import com.tsp.jimi_api.configurations.MicrosoftOAuthProperties;
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
 * {@link CalendarProvider} backed by the user's Outlook / Microsoft 365 calendar
 * via Microsoft Graph. Connected only when OAuth is configured AND the user has
 * linked a Microsoft account; otherwise falls back to NEEDS_CONNECTION.
 */
@Component
public class MicrosoftCalendarProvider implements CalendarProvider {

    private final CalendarAccountService accounts;
    private final MicrosoftApiClient api;
    private final MicrosoftOAuthProperties props;
    private final String defaultTimezone = ZoneId.systemDefault().getId();

    public MicrosoftCalendarProvider(final CalendarAccountService accounts,
                                     final MicrosoftApiClient api,
                                     final MicrosoftOAuthProperties props) {
        this.accounts = accounts;
        this.api = api;
        this.props = props;
    }

    @Override
    public String id() {
        return MicrosoftTokenStore.MICROSOFT;
    }

    @Override
    public boolean isConnected(final String userId) {
        return props.isConfigured() && accounts.isConnected(userId, id());
    }

    @Override
    public List<CalendarEvent> findEvents(final String userId, final String fromIso, final String toIso) {
        JSONObject response = api.listEvents(userId, fromIso, toIso);
        List<CalendarEvent> events = new ArrayList<>();
        JSONArray items = response.optJSONArray("value");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                events.add(MicrosoftEventMapper.toCalendarEvent(items.getJSONObject(i)));
            }
        }
        return events;
    }

    @Override
    public CalendarEvent create(final String userId, final EventDraft draft) {
        JSONObject created = api.insertEvent(userId,
                MicrosoftEventMapper.toGraphEvent(draft, defaultTimezone));
        return MicrosoftEventMapper.toCalendarEvent(created);
    }

    @Override
    public CalendarEvent update(final String userId, final String eventId, final EventDraft changes) {
        JSONObject patched = api.patchEvent(userId, eventId,
                MicrosoftEventMapper.toGraphEvent(changes, defaultTimezone));
        return MicrosoftEventMapper.toCalendarEvent(patched);
    }

    @Override
    public void delete(final String userId, final String eventId) {
        api.deleteEvent(userId, eventId);
    }
}
