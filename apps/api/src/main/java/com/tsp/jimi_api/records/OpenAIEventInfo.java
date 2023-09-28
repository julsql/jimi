package com.tsp.jimi_api.records;

import com.tsp.jimi_api.enums.Categories;
import com.tsp.jimi_api.enums.Type;
import org.json.JSONObject;

import java.sql.Date;
import java.sql.Time;
import java.util.List;

import static com.tsp.jimi_api.global.Shared.INDENT;
import static com.tsp.jimi_api.global.Shared.getTime;
import static com.tsp.jimi_api.global.Shared.getDate;

/**
 * The type Open ai event info.
 */
public class OpenAIEventInfo {
    /**
     * The Category.
     */
    private Categories category;
    /**
     * The Old value.
     */
    private Data oldValue;
    /**
     * The New value.
     */
    private Data newValue;
    /**
     * The Response.
     */
    private String response;
    /**
     * The User Id.
     */
    private String userId;

    /**
     * The Prompts.
     */
    private List<UserMessage> prompts;

    /**
     * Instantiates a new Open AI event info.
     *
     * @param prompts    the prompts
     * @param jsonString the json string
     * @param userId     the user id
     */
    public OpenAIEventInfo(final List<UserMessage> prompts, final String jsonString, final String userId) {
        try {
            this.userId = userId;
            this.prompts = prompts;

            try {
                JSONObject jsonObject = new JSONObject(jsonString);

                this.category = Categories.valueOf(jsonObject.getString("category"));
                this.response = jsonObject.getString("response");

                if (!jsonObject.isNull("old_value")) {

                    JSONObject oldValueJson = jsonObject.getJSONObject("old_value");

                    OpenAIEventInfo.Data oldValue = new OpenAIEventInfo.Data();
                    if (!oldValueJson.isEmpty()) {
                        try {
                            oldValue.setDate(getDate(oldValueJson.getString("date")));
                        } catch (Exception ignored) {
                        }
                        try {
                        oldValue.setBeginTime(getTime(oldValueJson.getString("begin_time")));
                        } catch (Exception ignored) {
                        }
                        try {
                        oldValue.setEndTime(getTime(oldValueJson.getString("end_time")));
                        } catch (Exception ignored) {
                        }
                        try {
                        oldValue.setType(Type.valueOf(oldValueJson.getString("type")));
                        } catch (Exception ignored) {
                        }
                        try {
                        oldValue.setTitle(oldValueJson.getString("title"));
                        } catch (Exception ignored) {
                        }
                    }
                    this.oldValue = oldValue;
                }

                if (!jsonObject.isNull("new_value")) {
                    JSONObject newValueJson = jsonObject.getJSONObject("new_value");

                    OpenAIEventInfo.Data newValue = new OpenAIEventInfo.Data();
                    if (!newValueJson.isEmpty()) {
                        try {
                            newValue.setDate(getDate(newValueJson.getString("date")));
                        } catch (Exception ignored) {
                        }
                        try {
                            newValue.setBeginTime(getTime(newValueJson.getString("begin_time")));
                        } catch (Exception ignored) {
                        }
                        try {
                            newValue.setEndTime(getTime(newValueJson.getString("end_time")));
                        } catch (Exception ignored) {
                        }
                        try {
                            newValue.setType(Type.valueOf(newValueJson.getString("type")));
                        } catch (Exception ignored) {
                        }
                        try {
                            newValue.setTitle(newValueJson.getString("title"));
                        } catch (Exception ignored) {
                        }
                    }

                    this.newValue = newValue;
                }
            } catch (Exception e) {
                this.category = Categories.OTHER;
                this.response = jsonString;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters et setters

    /**
     * Gets category.
     *
     * @return the category
     */
    public Categories getCategory() {
        return category;
    }

    /**
     * Sets category.
     *
     * @param category the category
     */
    public void setCategory(final Categories category) {
        this.category = category;
    }

    /**
     * Gets old value.
     *
     * @return the old value
     */
    public Data getOldValue() {
        return oldValue;
    }

    /**
     * Sets old value.
     *
     * @param oldValue the old value
     */
    public void setOldValue(final Data oldValue) {
        this.oldValue = oldValue;
    }

    /**
     * Gets new value.
     *
     * @return the new value
     */
    public Data getNewValue() {
        return newValue;
    }

    /**
     * Sets new value.
     *
     * @param newValue the new value
     */
    public void setNewValue(final Data newValue) {
        this.newValue = newValue;
    }

    /**
     * Gets response.
     *
     * @return the response
     */
    public String getResponse() {
        return response;
    }

    /**
     * Sets response.
     *
     * @param response the response
     */
    public void setResponse(final String response) {
        this.response = response;
    }

    /**
     * Gets user id.
     *
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets prompts.
     *
     * @return the prompts
     */
    public List<UserMessage> getPrompts() {
        return prompts;
    }

    /**
     * Sets prompts.
     *
     * @param prompts the prompts
     */
    public void setPrompts(final List<UserMessage> prompts) {
        this.prompts = prompts;
    }

    /**
     * Sets user id.
     *
     * @param userId the user id
     */
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    /**
     * The type Data.
     */
    public static class Data {
        /**
         * The Date.
         */
        private Date date;
        /**
         * The Beginning time.
         */
        private Time beginTime;
        /**
         * The End time.
         */
        private Time endTime;
        /**
         * The Type.
         */
        private Type type;
        /**
         * The Title.
         */
        private String title;

        // Getters et setters

        /**
         * Gets date.
         *
         * @return the date
         */
        public Date getDate() {
            return date;
        }

        /**
         * Sets date.
         *
         * @param date the date
         */
        public void setDate(final Date date) {
            this.date = date;
        }

        /**
         * Gets begin time.
         *
         * @return the beginning time
         */
        public Time getBeginTime() {
            return beginTime;
        }

        /**
         * Sets begin time.
         *
         * @param beginTime the beginning time
         */
        public void setBeginTime(final Time beginTime) {
            this.beginTime = beginTime;
        }

        /**
         * Gets end time.
         *
         * @return the end time
         */
        public Time getEndTime() {
            return endTime;
        }

        /**
         * Sets end time.
         *
         * @param endTime the end time
         */
        public void setEndTime(final Time endTime) {
            this.endTime = endTime;
        }

        /**
         * Gets type.
         *
         * @return the type
         */
        public Type getType() {
            return type;
        }

        /**
         * Sets type.
         *
         * @param type the type
         */
        public void setType(final Type type) {
            this.type = type;
        }

        /**
         * Gets title.
         *
         * @return the title
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets title.
         *
         * @param title the title
         */
        public void setTitle(final String title) {
            this.title = title;
        }

        /**
         * To json json object.
         *
         * @return the json object
         */
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

        @Override
        public String toString() {
            return this.toJson().toString(INDENT);
        }
    }

    /**
     * To json json object.
     *
     * @return the json object
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        json.put("category", category.toString());
        json.put("response", response);
        json.put("userId", userId);
        json.put("prompts", prompts);
        if (oldValue != null) {
            JSONObject oldValueJson = oldValue.toJson();
            json.put("old_value", oldValueJson);
        }
        if (newValue != null) {
            JSONObject newValueJson = newValue.toJson();
            json.put("new_value", newValueJson);
        }

        return json;
    }

    @Override
    public String toString() {
        return this.toJson().toString(INDENT);
    }
}
