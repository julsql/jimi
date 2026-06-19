package com.tsp.jimi_api.enums;

/**
 * The enum ConversationStatus.
 *
 * Tracks the lifecycle of a chat interaction that may span multiple HTTP
 * exchanges — when the LLM needs missing fields, or when a destructive action
 * must be confirmed by the user before it touches the real calendar.
 */
public enum ConversationStatus {
    /**
     * Draft is incomplete, waiting for the user to provide more information.
     */
    AWAITING_INFO,
    /**
     * A destructive action (edit/delete) has been resolved to concrete events
     * and is waiting for the user to explicitly confirm it via POST /chat/confirm.
     */
    AWAITING_CONFIRMATION,
    /**
     * The user has no calendar linked yet; the app should offer to connect one.
     */
    NEEDS_CONNECTION,
    /**
     * Draft is complete and the resulting calendar action has been performed.
     */
    COMPLETED,
    /**
     * A pending action was explicitly declined by the user; nothing happened.
     */
    CANCELLED
}
