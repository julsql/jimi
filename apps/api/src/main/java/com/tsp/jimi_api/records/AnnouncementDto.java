package com.tsp.jimi_api.records;

import com.tsp.jimi_api.entities.Announcement;
import com.tsp.jimi_api.enums.AnnouncementLevel;

/**
 * Read-only view of one announcement, returned by {@code GET /announcement}.
 * The {@code id} is what the app stores when the user taps "Don't show again".
 */
public record AnnouncementDto(
        Long id,
        String message,
        AnnouncementLevel level,
        String createdAt) {

    public static AnnouncementDto from(final Announcement a) {
        return new AnnouncementDto(
                a.getId(),
                a.getMessage(),
                a.getLevel(),
                a.getCreatedAt() == null ? null : a.getCreatedAt().toString());
    }
}
