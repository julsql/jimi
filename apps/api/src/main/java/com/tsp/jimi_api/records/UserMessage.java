package com.tsp.jimi_api.records;

import org.json.JSONObject;

/**
 * NewElevator record used to encapsulate data.
 *
 * @param sender  sender
 * @param message message
 */
public record UserMessage(String sender, String message) {
    /**
     * To json json object.
     *
     * @return the json object
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("sender", sender);
        json.put("message", message);
        return json;
    }
}
