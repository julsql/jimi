package com.tsp.jimi_api.repositories;

import com.tsp.jimi_api.entities.ChatContext;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Stores the rolling per-user conversation memory (one row per user).
 */
@Repository
public interface ChatContextRepository extends CrudRepository<ChatContext, String> {

    @Modifying
    @Query("DELETE FROM ChatContext c WHERE c.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);

    /** Drops conversation memory not updated since {@code threshold}. */
    @Modifying
    @Query("DELETE FROM ChatContext c WHERE c.updatedAt < :threshold")
    int deleteByUpdatedAtBefore(@Param("threshold") Instant threshold);
}
