package com.tutorial.redis.module09.domain.port.outbound;

import com.tutorial.redis.module09.domain.model.ReplicationInfo;

import java.util.Map;

/**
 * Outbound port for querying Redis replication information and performing
 * basic read/write operations for replication testing.
 *
 * <p>Implemented by a Redis adapter that issues INFO replication commands
 * and standard GET/SET operations against a running Redis instance.</p>
 */
public interface ReplicationInfoPort {

    /**
     * Retrieves the current replication information from the Redis server.
     *
     * @return a {@link ReplicationInfo} populated from the INFO replication command
     */
    ReplicationInfo getReplicationInfo();

    /**
     * Retrieves Redis server information for the specified section.
     *
     * @param section the INFO section to retrieve (e.g. "replication", "server", "memory")
     * @return a map of key-value pairs from the INFO output
     */
    Map<String, String> getServerInfo(String section);

    /**
     * Writes a key-value pair to Redis (always goes to master).
     *
     * @param key   the key to write
     * @param value the value to write
     */
    void writeData(String key, String value);

    /**
     * Reads a value from Redis by key.
     *
     * @param key the key to read
     * @return the value associated with the key, or null if not found
     */
    String readData(String key);
}
