package com.tutorial.redis.module13.domain.port.inbound;

import com.tutorial.redis.module13.domain.model.MemoryInfo;
import com.tutorial.redis.module13.domain.model.ServerMetrics;
import com.tutorial.redis.module13.domain.model.SlowLogEntry;

import java.util.List;

/**
 * Inbound port for Redis monitoring use cases.
 *
 * <p>Exposes operations for querying memory usage, slow log entries,
 * server performance metrics, and keyspace statistics.</p>
 */
public interface MonitoringUseCase {

    /**
     * Retrieves current memory usage information.
     *
     * @return memory info snapshot
     */
    MemoryInfo getMemoryInfo();

    /**
     * Retrieves the most recent slow log entries.
     *
     * @param count the maximum number of entries to return
     * @return list of slow log entries
     */
    List<SlowLogEntry> getSlowLog(int count);

    /**
     * Retrieves aggregated server performance metrics.
     *
     * @return server metrics snapshot
     */
    ServerMetrics getServerMetrics();

    /**
     * Returns the total number of keys stored in Redis.
     *
     * @return total key count
     */
    long getKeyCount();
}
