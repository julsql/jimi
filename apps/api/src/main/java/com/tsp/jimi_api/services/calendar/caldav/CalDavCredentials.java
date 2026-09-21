package com.tsp.jimi_api.services.calendar.caldav;

import org.json.JSONObject;

/**
 * Raw CalDAV credentials. CalDAV is HTTP Basic auth (username + app-specific
 * password) against a calendar <em>collection</em> URL the user supplies
 * directly — there is no bearer token to store/refresh, so unlike Google these
 * are serialized to a small JSON blob and encrypted whole into the
 * {@code access_token_enc} column.
 *
 * @param server   full CalDAV collection URL (e.g. iCloud home calendar)
 * @param username account login
 * @param password app-specific password
 */
public record CalDavCredentials(String server, String username, String password) {

    public String toJson() {
        return new JSONObject()
                .put("server", server)
                .put("username", username)
                .put("password", password)
                .toString();
    }

    public static CalDavCredentials fromJson(final String json) {
        JSONObject o = new JSONObject(json);
        return new CalDavCredentials(
                o.optString("server", null),
                o.optString("username", null),
                o.optString("password", null));
    }

    /** Collection URL guaranteed to end with a single trailing slash. */
    public String collectionUrl() {
        if (server == null) {
            return null;
        }
        return server.endsWith("/") ? server : server + "/";
    }
}
