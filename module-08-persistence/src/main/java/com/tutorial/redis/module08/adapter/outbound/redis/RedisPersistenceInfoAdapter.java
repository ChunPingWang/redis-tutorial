package com.tutorial.redis.module08.adapter.outbound.redis;

import com.tutorial.redis.module08.domain.model.PersistenceStatus;
import com.tutorial.redis.module08.domain.port.outbound.PersistenceInfoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Redis adapter that implements {@link PersistenceInfoPort} using low-level
 * Redis server commands.
 *
 * <p>Retrieves persistence status by issuing the {@code INFO persistence}
 * command and parsing the response. Also supports triggering background
 * RDB snapshots ({@code BGSAVE}) and AOF rewrites ({@code BGREWRITEAOF}).</p>
 *
 * <p>Uses {@link StringRedisTemplate} to obtain the underlying
 * {@link org.springframework.data.redis.connection.RedisConnection} for
 * server-level commands that have no high-level template equivalent.</p>
 */
@Component
public class RedisPersistenceInfoAdapter implements PersistenceInfoPort {

    private static final Logger log = LoggerFactory.getLogger(RedisPersistenceInfoAdapter.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisPersistenceInfoAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Retrieves the current persistence status from the Redis server by
     * issuing {@code INFO persistence} and parsing relevant fields.
     *
     * <p>Parsed fields:
     * <ul>
     *   <li>{@code rdb_last_save_time} — epoch seconds of the last RDB save</li>
     *   <li>{@code aof_enabled} — whether AOF is active (0 or 1)</li>
     *   <li>{@code loading} — whether Redis is replaying persistence files (0 or 1)</li>
     *   <li>{@code rdb_last_bgsave_status} — "ok" or an error message</li>
     *   <li>{@code aof_current_size} — current AOF file size in bytes</li>
     * </ul>
     *
     * @return a {@link PersistenceStatus} populated from the INFO output
     */
    @Override
    public PersistenceStatus getPersistenceStatus() {
        log.debug("Retrieving persistence status from Redis INFO persistence");

        Map<String, String> info = getServerInfo("persistence");

        boolean aofEnabled = "1".equals(info.getOrDefault("aof_enabled", "0"));
        long rdbLastSaveTime = parseLong(info.getOrDefault("rdb_last_save_time", "0"));
        boolean loading = "1".equals(info.getOrDefault("loading", "0"));
        String lastBgsaveStatus = info.getOrDefault("rdb_last_bgsave_status", "ok");
        long aofCurrentSize = parseLong(info.getOrDefault("aof_current_size", "0"));

        // RDB is considered enabled if there has ever been a successful save
        boolean rdbEnabled = rdbLastSaveTime > 0;

        PersistenceStatus status = new PersistenceStatus(
                rdbEnabled, aofEnabled, rdbLastSaveTime,
                aofCurrentSize, loading, lastBgsaveStatus
        );

        log.debug("Persistence status retrieved: {}", status);
        return status;
    }

    /**
     * Retrieves Redis server information for the specified section by issuing
     * {@code INFO <section>} and parsing the response into a key-value map.
     *
     * <p>The Spring Data Redis {@code serverCommands().info(section)} method
     * returns a {@link Properties} object. Each property entry is converted
     * to a {@code Map<String, String>} entry.</p>
     *
     * @param section the INFO section to retrieve (e.g. "persistence", "server", "memory")
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
     * Triggers an asynchronous RDB snapshot by issuing the {@code BGSAVE} command.
     * Redis will fork a child process to write the dump.rdb file.
     */
    @Override
    public void triggerBgsave() {
        log.info("Triggering BGSAVE (background RDB snapshot)");
        stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .bgSave();
        log.info("BGSAVE command sent successfully");
    }

    /**
     * Triggers an asynchronous AOF rewrite by issuing the {@code BGREWRITEAOF} command.
     * Redis will fork a child process to rewrite the append-only file.
     */
    @Override
    public void triggerBgrewriteaof() {
        log.info("Triggering BGREWRITEAOF (background AOF rewrite)");
        stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .bgReWriteAof();
        log.info("BGREWRITEAOF command sent successfully");
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
