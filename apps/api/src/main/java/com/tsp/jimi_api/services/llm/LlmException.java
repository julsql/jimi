package com.tsp.jimi_api.services.llm;

/**
 * Wraps any failure originating from an LLM call so callers can map it to
 * a clean HTTP error without depending on Spring web exceptions.
 */
public class LlmException extends RuntimeException {

    public LlmException(final String message) {
        super(message);
    }

    public LlmException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
