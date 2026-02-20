package com.tutorial.redis.module13.domain.port.outbound;

import com.tutorial.redis.module13.domain.model.MemoryInfo;
import com.tutorial.redis.module13.domain.model.ServerMetrics;
import com.tutorial.redis.module13.domain.model.SlowLogEntry;

import java.util.List;

/**
 * Outbound port for Redis monitoring and diagnostics.
 *
 * <p>Implemented by a Redis adapter that queries memory information,
 * slow log entries, server metrics, and keyspace statistics using
 * INFO commands and Lua scripts.</p>
 */
public interface MonitoringPort {

    /**
     * Retrieves current memory usage information from {@code INFO memory}.
     *
     * @return a snapshot of memory statistics including used, peak, max,
     *         eviction policy, and usage percentage
     */
    MemoryInfo getMemoryInfo();

    /**
     * Retrieves the most recent slow log entries via {@code SLOWLOG GET}.
     *
     * @param count the maximum number of entries to retrieve
     * @return list of slow log entries, most recent first
     */
    List<SlowLogEntry> getSlowLog(int count);

    /**
     * Aggregates server performance metrics from multiple INFO sections.
     *
     * @return a snapshot of server metrics (clients, hit rate, ops/sec, uptime)
     */
    ServerMetrics getServerMetrics();

    /**
     * Returns the total number of keys across all databases.
     *
     * @return total key count parsed from {@code INFO keyspace}
     */
    long getKeyCount();
}
