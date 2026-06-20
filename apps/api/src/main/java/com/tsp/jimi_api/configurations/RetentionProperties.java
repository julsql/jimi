package com.tsp.jimi_api.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Data-retention policy, bound from {@code retention.*}.
 *
 * <ul>
 *   <li>{@code user-days} — delete a user and ALL their data (events,
 *       conversations, memory, revoked calendar tokens) after this many days of
 *       inactivity.</li>
 *   <li>{@code context-days} — drop the rolling conversation memory after this
 *       many days, even for still-active users (privacy: don't keep chat history
 *       forever).</li>
 *   <li>{@code enabled} — turn the scheduled purge on/off.</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "retention")
public class RetentionProperties {

    private boolean enabled = true;
    private int userDays = 180;
    private int contextDays = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public int getUserDays() {
        return userDays;
    }

    public void setUserDays(final int userDays) {
        this.userDays = userDays;
    }

    public int getContextDays() {
        return contextDays;
    }

    public void setContextDays(final int contextDays) {
        this.contextDays = contextDays;
    }
}
