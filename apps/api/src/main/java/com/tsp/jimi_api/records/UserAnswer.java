package com.tsp.jimi_api.records;

import org.json.JSONObject;

/**
 * NewElevator record used to encapsulate data.
 *
 * @param message message
 */
public record UserAnswer(String message) {

    /**
     * To json json object.
     *
     * @return the json object
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("message", message);
        return json;
    }
}
