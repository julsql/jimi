package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.AppUser;
import com.tsp.jimi_api.repositories.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Records that a user was active, so inactive users can be purged later.
 */
@Service
public class UserActivityService {

    private final AppUserRepository repository;

    public UserActivityService(final AppUserRepository repository) {
        this.repository = repository;
    }

    /** Marks {@code userId} as seen now (creates the row on first contact). */
    @Transactional
    public void touch(final String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        Instant now = Instant.now();
        AppUser user = repository.findById(userId).orElseGet(() -> new AppUser(userId, now));
        user.setLastSeenAt(now);
        repository.save(user);
    }
}
