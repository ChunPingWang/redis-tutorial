package com.tutorial.redis.module08.domain.port.outbound;

import com.tutorial.redis.module08.domain.model.PersistenceStatus;

import java.util.Map;

/**
 * Outbound port for querying Redis persistence information and triggering
 * persistence operations.
 *
 * <p>Implemented by a Redis adapter that issues INFO, BGSAVE, and
 * BGREWRITEAOF commands against a running Redis instance.</p>
 */
public interface PersistenceInfoPort {

    /**
     * Retrieves the current persistence status from the Redis server.
     *
     * @return a {@link PersistenceStatus} populated from the INFO command
     */
    PersistenceStatus getPersistenceStatus();

    /**
     * Retrieves Redis server information for the specified section.
     *
     * @param section the INFO section to retrieve (e.g. "persistence", "server", "memory")
     * @return a map of key-value pairs from the INFO output
     */
    Map<String, String> getServerInfo(String section);

    /**
     * Triggers an asynchronous RDB snapshot (BGSAVE command).
     * Redis will fork a child process to write the dump.rdb file.
     */
    void triggerBgsave();

    /**
     * Triggers an asynchronous AOF rewrite (BGREWRITEAOF command).
     * Redis will fork a child process to rewrite the AOF file.
     */
    void triggerBgrewriteaof();
}
