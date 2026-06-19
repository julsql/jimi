package com.tsp.jimi_api.records;

import com.tsp.jimi_api.enums.Categories;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A destructive calendar action (EDIT or DELETE) that has been resolved to
 * concrete event ids and is now <em>waiting for the user to confirm</em>.
 *
 * <p>This is the heart of the "no bad action" guarantee. When the LLM infers
 * an edit/delete, the server does NOT touch the calendar — it resolves the
 * target event(s), serialises this object into the conversation, and asks the
 * user to confirm. Execution (via {@code POST /chat/confirm}) reads this object
 * back and acts on the recorded ids only. The LLM is never in the confirmation
 * loop, so it cannot change what gets executed after the user has seen it.
 *
 * @param category  EDIT or DELETE
 * @param eventIds  the exact provider event ids that will be affected
 * @param changes   field changes to apply (EDIT only; ignored for DELETE)
 * @param summary   the human-readable description shown to the user to confirm
 */
public record ProposedAction(
        Categories category,
        List<String> eventIds,
        EventDraft changes,
        String summary) {

    public ProposedAction {
        eventIds = eventIds == null ? List.of() : List.copyOf(eventIds);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("category", category.name());
        json.put("eventIds", new JSONArray(eventIds));
        json.put("changes", changes == null ? new JSONObject() : changes.toJson());
        json.put("summary", summary);
        return json;
    }

    public static ProposedAction fromJson(final String raw) {
        JSONObject json = new JSONObject(raw);
        List<String> ids = new ArrayList<>();
        JSONArray array = json.optJSONArray("eventIds");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                ids.add(array.getString(i));
            }
        }
        return new ProposedAction(
                Categories.valueOf(json.getString("category")),
                ids,
                EventDraft.parse(json.optJSONObject("changes")),
                json.optString("summary", ""));
    }
}
