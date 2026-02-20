package com.tutorial.redis.module08.domain.port.inbound;

import com.tutorial.redis.module08.domain.model.PersistenceStatus;

/**
 * Inbound port: querying persistence status and triggering persistence operations.
 *
 * <p>Provides the application-level API for inspecting how the current
 * Redis instance is configured for persistence and for manually
 * triggering snapshot or rewrite operations.</p>
 */
public interface PersistenceInfoUseCase {

    /**
     * Retrieves the current persistence status from the connected Redis instance.
     *
     * @return the current {@link PersistenceStatus}
     */
    PersistenceStatus getPersistenceStatus();

    /**
     * Triggers an RDB background save (BGSAVE).
     * Redis forks a child process to create a point-in-time snapshot.
     */
    void triggerRdbSnapshot();

    /**
     * Triggers an AOF background rewrite (BGREWRITEAOF).
     * Redis forks a child process to compact the append-only file.
     */
    void triggerAofRewrite();
}
