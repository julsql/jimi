package com.tsp.jimi_api.enums;

/**
 * Criticality of a broadcast announcement shown in the app popup. Drives the
 * popup styling on the client (colour / icon); the server only stores it.
 */
public enum AnnouncementLevel {
    /** Neutral information (new feature, tip…). */
    INFO,
    /** Something the user should be aware of (degraded service, maintenance soon…). */
    WARNING,
    /** Service down / breaking issue — highest emphasis. */
    CRITICAL
}
