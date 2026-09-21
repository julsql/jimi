package com.tsp.jimi_api.services.calendar;

/**
 * Raised when an operation needs a calendar but the user has not linked one
 * yet. {@code ChatService} catches this and returns a NEEDS_CONNECTION
 * response so the app can offer a one-tap "Connect your calendar" button.
 */
public class CalendarNotConnectedException extends RuntimeException {

    public CalendarNotConnectedException(final String userId) {
        super("No calendar connected for user " + userId);
    }
}
