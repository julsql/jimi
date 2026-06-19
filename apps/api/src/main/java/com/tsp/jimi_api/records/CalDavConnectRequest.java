package com.tsp.jimi_api.records;

/**
 * Request payload for {@code POST /connect/caldav}.
 *
 * <p>CalDAV is HTTP Basic auth against a calendar <em>collection</em> URL the
 * user supplies directly (e.g. iCloud
 * {@code https://caldav.icloud.com/<id>/calendars/home/}); there is no OAuth
 * flow. The password is an app-specific password where the provider requires
 * one.
 *
 * @param userId    identifies the user owning the calendar link (required)
 * @param serverUrl full CalDAV collection URL (required)
 * @param username  account login (required)
 * @param password  app-specific password (required)
 */
public record CalDavConnectRequest(String userId, String serverUrl, String username, String password) {
}
