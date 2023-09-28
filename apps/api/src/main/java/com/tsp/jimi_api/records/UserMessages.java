package com.tsp.jimi_api.records;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * NewElevator record used to encapsulate data.
 *
 * @param userId       user id
 * @param userMessages user messages
 */
public record UserMessages(String userId, List<UserMessage> userMessages) {
    /**
     * To json json object.
     *
     * @return the json object
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("userId", userId);
        JSONArray messageArray = new JSONArray();
        for (UserMessage userMessage : userMessages) {
            messageArray.put(userMessage.toJson());
        }
        json.put("userMessages", messageArray);
        return json;
    }
}
