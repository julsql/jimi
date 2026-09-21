-- Table de verrous ShedLock (voir SchedulerLockConfig).
-- Creee au demarrage via spring.sql.init.mode=always : ddl-auto=update ne la
-- genere pas car ce n'est pas une entite JPA. Idempotent (IF NOT EXISTS).
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
