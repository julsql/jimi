package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.Announcement;
import com.tsp.jimi_api.enums.AnnouncementLevel;
import com.tsp.jimi_api.repositories.AnnouncementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Manages broadcast announcements: the app reads the latest active one, admins
 * post new ones and retire old ones. No per-user state — "don't show again" is
 * tracked client-side per announcement id.
 */
@Service
public class AnnouncementService {

    private final AnnouncementRepository repository;

    public AnnouncementService(final AnnouncementRepository repository) {
        this.repository = repository;
    }

    /** The announcement the app should currently show, if any. */
    public Optional<Announcement> getLatestActive() {
        return repository.findFirstByActiveTrueOrderByCreatedAtDesc();
    }

    /** Posts a new active announcement; it immediately becomes the latest one. */
    public Announcement create(final String message, final AnnouncementLevel level) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        AnnouncementLevel resolved = level == null ? AnnouncementLevel.INFO : level;
        return repository.save(new Announcement(message.strip(), resolved, Instant.now()));
    }

    /**
     * Retires an announcement so it is no longer served. Returns true if an
     * announcement with that id existed.
     */
    public boolean deactivate(final Long id) {
        Optional<Announcement> found = repository.findById(id);
        found.ifPresent(a -> {
            a.setActive(false);
            repository.save(a);
        });
        return found.isPresent();
    }
}
