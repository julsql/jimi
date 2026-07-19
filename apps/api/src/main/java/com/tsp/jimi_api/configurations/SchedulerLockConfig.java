package com.tsp.jimi_api.configurations;

import javax.sql.DataSource;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verrou distribue (ShedLock) pour les taches {@code @Scheduled}.
 *
 * <p>Garantit qu'un job planifie ne s'execute que sur une seule instance a la
 * fois, meme si plusieurs pods tournent en parallele. Permet donc de deployer
 * jimi-api en RollingUpdate (rollout sans coupure) sans que la purge de
 * retention ne tourne en double.
 *
 * <p>Le verrou est stocke en base (table {@code shedlock}, cf.
 * {@code schema.sql}) ; {@code usingDbTime()} s'appuie sur l'horloge MariaDB.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(final DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build());
    }
}
