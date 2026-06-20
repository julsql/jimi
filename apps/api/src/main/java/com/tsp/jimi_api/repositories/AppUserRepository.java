package com.tsp.jimi_api.repositories;

import com.tsp.jimi_api.entities.AppUser;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Tracks anonymous users' last activity for data-retention purges.
 */
@Repository
public interface AppUserRepository extends CrudRepository<AppUser, String> {

    /** Users not seen since {@code threshold} — candidates for deletion. */
    List<AppUser> findByLastSeenAtBefore(Instant threshold);

    @Modifying
    @Query("DELETE FROM AppUser u WHERE u.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}
