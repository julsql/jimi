package com.tsp.jimi_api.records;

import org.json.JSONObject;

/**
 * Error record used in REST endpoints.
 *
 * @param error  type.
 * @param reason of the error.
 */
public record Error(String error, String reason) {
    /**
     * To json json object.
     *
     * @return JSON -serialized Error record.
     */
    public JSONObject toJson() {
        return new JSONObject() {{
            put("error", error);
            put("reason", reason);
        }};
    }
}
