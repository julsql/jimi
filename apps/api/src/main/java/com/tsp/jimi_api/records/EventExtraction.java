package com.tsp.jimi_api.records;

import com.tsp.jimi_api.enums.Categories;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed structured output produced by the LLM for one chat turn.
 *
 * <p>Maps the JSON contract defined in {@link com.tsp.jimi_api.global.Prompts}
 * into typed fields. {@code old_value} / {@code new_value} are
 * {@link EventDraft}s carrying the full set of native calendar fields
 * (title, start/end, all-day, location, attendees, description, recurrence,
 * reminder, type).
 *
 * <p>On any parse error we fall back to {@link Categories#OTHER} with empty
 * drafts, so a malformed model reply degrades to a harmless off-topic answer
 * rather than a wrong calendar action.
 */
public class EventExtraction {

    private Categories category;
    private EventDraft oldValue;
    private EventDraft newValue;
    private final List<String> missingFields = new ArrayList<>();
    private String response;

    public EventExtraction(final String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            this.category = parseCategory(json.optString("category", "OTHER"));
            this.response = json.optString("response", "");
            this.oldValue = EventDraft.parse(json.optJSONObject("old_value"));
            this.newValue = EventDraft.parse(json.optJSONObject("new_value"));

            JSONArray missing = json.optJSONArray("missing_fields");
            if (missing != null) {
                for (int i = 0; i < missing.length(); i++) {
                    this.missingFields.add(missing.getString(i));
                }
            }
        } catch (Exception e) {
            this.category = Categories.OTHER;
            this.response = jsonString;
            this.oldValue = EventDraft.empty();
            this.newValue = EventDraft.empty();
        }
    }

    private static Categories parseCategory(final String raw) {
        try {
            return Categories.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return Categories.OTHER;
        }
    }

    public Categories getCategory() {
        return category;
    }

    public void setCategory(final Categories category) {
        this.category = category;
    }

    public EventDraft getOldValue() {
        return oldValue;
    }

    public EventDraft getNewValue() {
        return newValue;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public String getResponse() {
        return response;
    }
}
