package com.tsp.jimi_api.services;

import com.tsp.jimi_api.configurations.RetentionProperties;
import com.tsp.jimi_api.entities.AppUser;
import com.tsp.jimi_api.repositories.AppUserRepository;
import com.tsp.jimi_api.repositories.ChatContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled data-retention purge:
 * <ul>
 *   <li>deletes users (and ALL their data, revoking calendar tokens) that have
 *       been inactive for {@code retention.user-days};</li>
 *   <li>drops conversation memory older than {@code retention.context-days},
 *       even for still-active users.</li>
 * </ul>
 *
 * <p>Runs daily at 03:30 (server time). Disable with {@code retention.enabled=false}.
 */
@Service
public class RetentionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetentionService.class);

    private final RetentionProperties properties;
    private final AppUserRepository appUserRepository;
    private final ChatContextRepository chatContextRepository;
    private final UserService userService;

    public RetentionService(final RetentionProperties properties,
                            final AppUserRepository appUserRepository,
                            final ChatContextRepository chatContextRepository,
                            final UserService userService) {
        this.properties = properties;
        this.appUserRepository = appUserRepository;
        this.chatContextRepository = chatContextRepository;
        this.userService = userService;
    }

    @Scheduled(cron = "${retention.cron:0 30 3 * * *}")
    public void purge() {
        if (!properties.isEnabled()) {
            return;
        }
        purgeStaleContexts();
        purgeInactiveUsers();
    }

    /** Deletes inactive users and everything tied to them. */
    public int purgeInactiveUsers() {
        Instant threshold = Instant.now().minus(properties.getUserDays(), ChronoUnit.DAYS);
        List<AppUser> inactive = appUserRepository.findByLastSeenAtBefore(threshold);
        for (AppUser user : inactive) {
            try {
                userService.deleteAllUserData(user.getUserId());
            } catch (Exception e) {
                LOGGER.warn("[retention] failed to purge user {}: {}", user.getUserId(), e.getMessage());
            }
        }
        if (!inactive.isEmpty()) {
            LOGGER.info("[retention] purged {} inactive user(s) (> {} days)",
                    inactive.size(), properties.getUserDays());
        }
        return inactive.size();
    }

    /** Drops conversation memory older than the context retention window. */
    public int purgeStaleContexts() {
        Instant threshold = Instant.now().minus(properties.getContextDays(), ChronoUnit.DAYS);
        int dropped = chatContextRepository.deleteByUpdatedAtBefore(threshold);
        if (dropped > 0) {
            LOGGER.info("[retention] dropped {} stale conversation memories (> {} days)",
                    dropped, properties.getContextDays());
        }
        return dropped;
    }
}
