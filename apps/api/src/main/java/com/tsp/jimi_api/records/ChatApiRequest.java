package com.tsp.jimi_api.records;

/**
 * Request payload from the frontend to POST /chat.
 *
 * @param userId         identifies the user owning the agenda (required)
 * @param message        the user's latest natural-language message (required)
 * @param conversationId optional - present when continuing an in-progress
 *                       event creation that needed more info on a previous turn
 */
public record ChatApiRequest(String userId, String message, String conversationId) {
}
