package com.tutorial.redis.module09.adapter.outbound.redis;

import com.tutorial.redis.module09.domain.model.ReplicationInfo;
import com.tutorial.redis.module09.domain.port.outbound.ReplicationInfoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Redis adapter that implements {@link ReplicationInfoPort} using low-level
 * Redis server commands.
 *
 * <p>Retrieves replication status by issuing the {@code INFO replication}
 * command and parsing the response. Also provides basic read/write operations
 * for demonstrating replication behaviour in a master-replica topology.</p>
 *
 * <p>Uses {@link StringRedisTemplate} to obtain the underlying
 * {@link org.springframework.data.redis.connection.RedisConnection} for
 * server-level commands that have no high-level template equivalent.</p>
 */
@Component
public class RedisReplicationInfoAdapter implements ReplicationInfoPort {

    private static final Logger log = LoggerFactory.getLogger(RedisReplicationInfoAdapter.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisReplicationInfoAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Retrieves the current replication status from the Redis server by
     * issuing {@code INFO replication} and parsing relevant fields.
     *
     * <p>Parsed fields:
     * <ul>
     *   <li>{@code role} — "master" or "slave"</li>
     *   <li>{@code connected_slaves} — number of connected replicas</li>
     *   <li>{@code master_repl_offset} — the master's replication offset</li>
     *   <li>{@code repl_backlog_size} — replication backlog buffer size</li>
     *   <li>{@code repl_backlog_active} — whether the backlog is active (0 or 1)</li>
     * </ul>
     *
     * @return a {@link ReplicationInfo} populated from the INFO output
     */
    @Override
    public ReplicationInfo getReplicationInfo() {
        log.debug("Retrieving replication info from Redis INFO replication");

        Map<String, String> info = getServerInfo("replication");

        String role = info.getOrDefault("role", "master");
        int connectedSlaves = parseInt(info.getOrDefault("connected_slaves", "0"));
        long replicationOffset = parseLong(info.getOrDefault("master_repl_offset", "0"));
        int replicationBacklogSize = parseInt(info.getOrDefault("repl_backlog_size", "0"));
        boolean replicationBacklogActive = "1".equals(info.getOrDefault("repl_backlog_active", "0"));

        ReplicationInfo replicationInfo = new ReplicationInfo(
                role, connectedSlaves, replicationOffset,
                replicationBacklogSize, replicationBacklogActive
        );

        log.debug("Replication info retrieved: {}", replicationInfo);
        return replicationInfo;
    }

    /**
     * Retrieves Redis server information for the specified section by issuing
     * {@code INFO <section>} and parsing the response into a key-value map.
     *
     * <p>The Spring Data Redis {@code serverCommands().info(section)} method
     * returns a {@link Properties} object. Each property entry is converted
     * to a {@code Map<String, String>} entry.</p>
     *
     * @param section the INFO section to retrieve (e.g. "replication", "server", "memory")
     * @return a map of key-value pairs from the INFO output
     */
    @Override
    public Map<String, String> getServerInfo(String section) {
        log.debug("Retrieving Redis INFO for section: {}", section);

        Properties properties = stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .info(section);

        Map<String, String> result = new LinkedHashMap<>();
        if (properties != null) {
            properties.forEach((key, value) ->
                    result.put(String.valueOf(key), String.valueOf(value)));
        }

        log.debug("Redis INFO {} returned {} entries", section, result.size());
        return result;
    }

    /**
     * Writes a key-value pair to Redis using the {@code SET} command.
     * In a replication topology, writes always go to the master.
     *
     * @param key   the key to write
     * @param value the value to write
     */
    @Override
    public void writeData(String key, String value) {
        log.debug("Writing data: key='{}', value='{}'", key, value);
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * Reads a value from Redis using the {@code GET} command.
     * In a replication topology with read-write splitting, reads can
     * be served by replicas depending on the configured strategy.
     *
     * @param key the key to read
     * @return the value associated with the key, or null if not found
     */
    @Override
    public String readData(String key) {
        log.debug("Reading data for key: '{}'", key);
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * Safely parses a string to an int value, returning 0 on failure.
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse int value: '{}'", value);
            return 0;
        }
    }

    /**
     * Safely parses a string to a long value, returning 0 on failure.
     */
    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long value: '{}'", value);
            return 0L;
        }
    }
}
