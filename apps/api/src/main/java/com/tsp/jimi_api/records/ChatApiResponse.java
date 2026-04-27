package com.tsp.jimi_api.records;

import com.tsp.jimi_api.enums.ConversationStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static com.tsp.jimi_api.global.Shared.INDENT;

/**
 * Response payload returned by POST /chat.
 *
 * The frontend keeps {@code conversationId} when {@code status} is
 * AWAITING_INFO and re-sends it with the user's next message. When the
 * status is COMPLETED the conversation is closed server-side.
 *
 * @param conversationId opaque ID for the in-progress draft, or null when
 *                       no follow-up is needed (GET / OTHER / completed)
 * @param status         AWAITING_INFO when the user must supply more data,
 *                       COMPLETED otherwise
 * @param message        human-readable assistant message to display
 * @param missingFields  field names still missing (empty unless AWAITING_INFO)
 */
public record ChatApiResponse(
        String conversationId,
        ConversationStatus status,
        String message,
        List<String> missingFields) {

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
        return json;
    }

    public String toJsonString() {
        return toJson().toString(INDENT);
    }
}
