package com.tsp.jimi_api.records;

import com.tsp.jimi_api.enums.ConversationStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static com.tsp.jimi_api.global.Shared.INDENT;

/**
 * Response payload returned by POST /chat and POST /chat/confirm.
 *
 * <p>The frontend keeps {@code conversationId} while the {@code status} needs a
 * follow-up:
 * <ul>
 *   <li>{@code AWAITING_INFO} — supply the missing fields,</li>
 *   <li>{@code AWAITING_CONFIRMATION} — show Confirm/Cancel; call
 *       {@code /chat/confirm} with the same conversationId,</li>
 *   <li>{@code NEEDS_CONNECTION} — offer to connect a calendar,</li>
 *   <li>{@code COMPLETED}/{@code CANCELLED} — drop the conversationId.</li>
 * </ul>
 *
 * @param conversationId opaque id for the in-progress interaction, or null
 * @param status         drives the frontend follow-up (see above)
 * @param message        human-readable assistant message to display
 * @param missingFields  field names still missing (empty unless AWAITING_INFO)
 * @param eventUrl       deep link to open the affected event in the native
 *                       calendar app, or null
 */
public record ChatApiResponse(
        String conversationId,
        ConversationStatus status,
        String message,
        List<String> missingFields,
        String eventUrl) {

    public ChatApiResponse(final String conversationId,
                           final ConversationStatus status,
                           final String message,
                           final List<String> missingFields) {
        this(conversationId, status, message, missingFields, null);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("conversationId", conversationId == null ? JSONObject.NULL : conversationId);
        json.put("status", status.name());
        json.put("message", message);
        JSONArray missing = new JSONArray();
        if (missingFields != null) {
            missingFields.forEach(missing::put);
        }
        json.put("missingFields", missing);
        json.put("eventUrl", eventUrl == null ? JSONObject.NULL : eventUrl);
        return json;
    }

    public String toJsonString() {
        return toJson().toString(INDENT);
    }
}
