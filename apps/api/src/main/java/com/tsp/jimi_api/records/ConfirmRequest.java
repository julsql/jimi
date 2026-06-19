package com.tsp.jimi_api.records;

/**
 * Request payload for POST /chat/confirm.
 *
 * <p>Sent when the user taps Confirm or Cancel on a destructive action that
 * JIMI proposed (status AWAITING_CONFIRMATION).
 *
 * @param userId         the user owning the pending action (required)
 * @param conversationId the AWAITING_CONFIRMATION conversation id (required)
 * @param confirmed      true to execute the action, false to discard it
 */
public record ConfirmRequest(String userId, String conversationId, boolean confirmed) {
}
