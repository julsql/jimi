package com.tsp.jimi_api.enums;

/**
 * The enum ConversationStatus.
 *
 * Tracks the lifecycle of an event-creation conversation that may span
 * multiple HTTP exchanges when the LLM needs to ask for missing fields.
 */
public enum ConversationStatus {
    /**
     * Draft is incomplete, waiting for the user to provide more information.
     */
    AWAITING_INFO,
    /**
     * Draft is complete and the resulting agenda action has been performed.
     */
    COMPLETED
}
