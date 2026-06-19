package com.tsp.jimi_api.repositories;

import com.tsp.jimi_api.entities.CalendarAccount;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Stores users' linked calendar accounts (encrypted OAuth tokens).
 */
@Repository
public interface CalendarAccountRepository extends CrudRepository<CalendarAccount, Long> {

    Optional<CalendarAccount> findByUserIdAndProvider(String userId, String provider);

    List<CalendarAccount> findByUserId(String userId);

    boolean existsByUserIdAndProvider(String userId, String provider);

    @Modifying
    @Query("DELETE FROM CalendarAccount a WHERE a.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}
