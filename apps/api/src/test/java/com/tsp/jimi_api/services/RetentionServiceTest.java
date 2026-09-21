package com.tsp.jimi_api.services;

import com.tsp.jimi_api.configurations.RetentionProperties;
import com.tsp.jimi_api.entities.AppUser;
import com.tsp.jimi_api.entities.ChatContext;
import com.tsp.jimi_api.support.InMemoryAppUserRepository;
import com.tsp.jimi_api.support.InMemoryChatContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RetentionServiceTest {

    private InMemoryAppUserRepository users;
    private InMemoryChatContextRepository contexts;
    private UserService userService;
    private RetentionService retention;

    @BeforeEach
    void setUp() {
        users = new InMemoryAppUserRepository();
        contexts = new InMemoryChatContextRepository();
        userService = mock(UserService.class);
        RetentionProperties props = new RetentionProperties();
        props.setUserDays(180);
        props.setContextDays(30);
        retention = new RetentionService(props, users, contexts, userService);
    }

    private void seedUser(final String id, final long daysAgo) {
        users.save(new AppUser(id, Instant.now().minus(daysAgo, ChronoUnit.DAYS)));
    }

    private void seedContext(final String id, final long daysAgo) {
        ChatContext c = new ChatContext(id);
        c.setUpdatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        contexts.save(c);
    }

    @Test
    void purgesUsersInactiveBeyondTheRetentionWindow() {
        seedUser("stale", 200);   // > 180 days
        seedUser("active", 5);

        retention.purgeInactiveUsers();

        verify(userService).deleteAllUserData("stale");
        verify(userService, never()).deleteAllUserData("active");
    }

    @Test
    void dropsConversationMemoryOlderThanTheContextWindow() {
        seedContext("old", 45);   // > 30 days
        seedContext("recent", 2);

        int dropped = retention.purgeStaleContexts();

        assertThat(dropped).isEqualTo(1);
        assertThat(contexts.existsById("old")).isFalse();
        assertThat(contexts.existsById("recent")).isTrue();
    }
}
