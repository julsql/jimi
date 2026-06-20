package com.tsp.jimi_api.records;

/**
 * Request payload from the frontend to POST /chat.
 *
 * @param userId         identifies the user owning the agenda (required)
 * @param message        the user's latest natural-language message (required)
 * @param conversationId optional - present when continuing an in-progress
 *                       event creation that needed more info on a previous turn
 * @param calendarMode   opt-in to the connected-calendar experience (Google /
 *                       CalDAV / Microsoft, with NEEDS_CONNECTION + confirmation).
 *                       Absent/false (the default, and what already-deployed apps
 *                       send) keeps the legacy behaviour: events live in JIMI's
 *                       own DB and edit/delete happen directly.
 * @param timezone       the user's IANA timezone (e.g. "Europe/Paris"), sent by
 *                       the client from the device. Used to resolve "today"/"now"
 *                       and as the default timezone for new events. Falls back to
 *                       the server's zone when absent.
 */
public record ChatApiRequest(String userId, String message, String conversationId,
                             boolean calendarMode, String timezone) {
}
