package com.tsp.jimi_api.records;

import com.tsp.jimi_api.enums.Categories;
import com.tsp.jimi_api.enums.Type;
import com.tsp.jimi_api.global.Shared;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * Parsed structured output produced by the LLM.
 *
 * Maps the JSON contract defined in {@link com.tsp.jimi_api.global.Prompts}
 * into typed fields. Replaces the old OpenAIEventInfo class - same role,
 * provider-neutral name and cleaner parsing.
 */
public class EventExtraction {

    private Categories category;
    private EventData oldValue;
    private EventData newValue;
    private List<String> missingFields = new ArrayList<>();
    private String response;

    public EventExtraction(final String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            this.category = parseCategory(json.optString("category", "OTHER"));
            this.response = json.optString("response", "");
            this.oldValue = EventData.parse(json.optJSONObject("old_value"));
            this.newValue = EventData.parse(json.optJSONObject("new_value"));

            JSONArray missing = json.optJSONArray("missing_fields");
            if (missing != null) {
                for (int i = 0; i < missing.length(); i++) {
                    this.missingFields.add(missing.getString(i));
                }
            }
        } catch (Exception e) {
            this.category = Categories.OTHER;
            this.response = jsonString;
            this.oldValue = new EventData();
            this.newValue = new EventData();
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

    public EventData getOldValue() {
        return oldValue;
    }

    public EventData getNewValue() {
        return newValue;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public String getResponse() {
        return response;
    }

    /**
     * Typed view of one event field block (old_value or new_value).
     */
    public static class EventData {
        private Date date;
        private Time beginTime;
        private Time endTime;
        private Type type;
        private String title;

        static EventData parse(final JSONObject json) {
            EventData data = new EventData();
            if (json == null || json.isEmpty()) {
                return data;
            }
            data.date = parseField(() -> Shared.getDate(json.getString("date")));
            data.beginTime = parseField(() -> Shared.getTime(json.getString("begin_time")));
            data.endTime = parseField(() -> Shared.getTime(json.getString("end_time")));
            data.type = parseField(() -> Type.valueOf(json.getString("type")));
            data.title = parseField(() -> json.getString("title"));
            return data;
        }

        private static <T> T parseField(final FieldParser<T> parser) {
            try {
                return parser.parse();
            } catch (Exception e) {
                return null;
            }
        }

        @FunctionalInterface
        private interface FieldParser<T> {
            T parse() throws Exception;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(final Date date) {
            this.date = date;
        }

        public Time getBeginTime() {
            return beginTime;
        }

        public void setBeginTime(final Time beginTime) {
            this.beginTime = beginTime;
        }

        public Time getEndTime() {
            return endTime;
        }

        public void setEndTime(final Time endTime) {
            this.endTime = endTime;
        }

        public Type getType() {
            return type;
        }

        public void setType(final Type type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public boolean isEmpty() {
            return date == null && beginTime == null && endTime == null && type == null && title == null;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            if (date != null) {
                json.put("date", date.toString());
            }
            if (beginTime != null) {
                json.put("begin_time", beginTime.toString());
            }
            if (endTime != null) {
                json.put("end_time", endTime.toString());
            }
            if (type != null) {
                json.put("type", type.toString());
            }
            if (title != null) {
                json.put("title", title);
            }
            return json;
        }
    }
}
