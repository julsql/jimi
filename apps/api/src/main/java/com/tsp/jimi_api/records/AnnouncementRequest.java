package com.tsp.jimi_api.records;

import com.tsp.jimi_api.enums.AnnouncementLevel;

/**
 * Admin payload for POST /admin/announcement.
 *
 * @param message the text shown in the app popup (required, non-blank)
 * @param level   criticality; defaults to INFO when omitted
 */
public record AnnouncementRequest(String message, AnnouncementLevel level) {
}
