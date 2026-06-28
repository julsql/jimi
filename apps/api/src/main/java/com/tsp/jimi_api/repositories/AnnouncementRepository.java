package com.tsp.jimi_api.repositories;

import com.tsp.jimi_api.entities.Announcement;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Persistence for broadcast announcements. The app reads only the latest active
 * one; the admin endpoints create and retire them.
 */
@Repository
public interface AnnouncementRepository extends CrudRepository<Announcement, Long> {

    /** The most recently created announcement that is still active, if any. */
    Optional<Announcement> findFirstByActiveTrueOrderByCreatedAtDesc();
}
