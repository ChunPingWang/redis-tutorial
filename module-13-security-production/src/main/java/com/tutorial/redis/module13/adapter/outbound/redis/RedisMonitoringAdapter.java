package com.tutorial.redis.module13.adapter.outbound.redis;

import com.tutorial.redis.module13.domain.model.EvictionPolicy;
import com.tutorial.redis.module13.domain.model.MemoryInfo;
import com.tutorial.redis.module13.domain.model.ServerMetrics;
import com.tutorial.redis.module13.domain.model.SlowLogEntry;
import com.tutorial.redis.module13.domain.port.outbound.MonitoringPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Redis adapter for monitoring and diagnostics operations.
 *
 * <p>Implements {@link MonitoringPort} using {@link StringRedisTemplate} to
 * query memory information, slow log entries, server metrics, and keyspace
 * statistics.</p>
 *
 * <p>Implementation details:</p>
 * <ul>
 *   <li>Memory info is parsed from {@code INFO memory}</li>
 *   <li>Slow log entries are retrieved via a Lua script wrapping {@code SLOWLOG GET}</li>
 *   <li>Server metrics are aggregated from {@code INFO stats}, {@code INFO clients},
 *       and {@code INFO server}</li>
 *   <li>Key count is extracted from {@code INFO keyspace}</li>
 * </ul>
 */
@Component
public class RedisMonitoringAdapter implements MonitoringPort {

    private static final Logger log = LoggerFactory.getLogger(RedisMonitoringAdapter.class);

    /**
     * Lua script that returns SLOWLOG GET results as a JSON-encoded array.
     * Each slow log entry is a nested array: [id, timestamp, duration, [cmd_args...], client_addr, ...]
     * cjson.encode handles the nested structure and returns valid JSON.
     */
    private static final DefaultRedisScript<String> SLOWLOG_GET_SCRIPT = new DefaultRedisScript<>(
            "local result = redis.call('SLOWLOG', 'GET', ARGV[1])\n" +
            "return cjson.encode(result)",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisMonitoringAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public MemoryInfo getMemoryInfo() {
        Properties memoryProps = executeInfo("memory");

        long usedMemory = parseLong(memoryProps, "used_memory", 0L);
        long maxMemory = parseLong(memoryProps, "maxmemory", 0L);
        long peakMemory = parseLong(memoryProps, "used_memory_peak", 0L);
        String policyName = memoryProps.getProperty("maxmemory_policy", "noeviction");

        EvictionPolicy evictionPolicy = EvictionPolicy.fromRedisName(policyName);
        double usagePercentage = maxMemory > 0
                ? (double) usedMemory / maxMemory * 100.0
                : 0.0;

        MemoryInfo info = new MemoryInfo(usedMemory, maxMemory, evictionPolicy,
                usagePercentage, peakMemory);
        log.debug("Memory info: {}", info);
        return info;
    }

    @Override
    public List<SlowLogEntry> getSlowLog(int count) {
        try {
            String json = stringRedisTemplate.execute(
                    SLOWLOG_GET_SCRIPT, Collections.emptyList(), String.valueOf(count));
            if (json == null || json.isEmpty() || "[]".equals(json.trim())) {
                log.debug("SLOWLOG GET returned empty result");
                return Collections.emptyList();
            }
            return parseSlowLogJson(json);
        } catch (Exception e) {
            log.warn("SLOWLOG GET failed: {}. Returning empty list.", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public ServerMetrics getServerMetrics() {
        Properties statsProps = executeInfo("stats");
        Properties clientsProps = executeInfo("clients");
        Properties serverProps = executeInfo("server");

        long connectedClients = parseLong(clientsProps, "connected_clients", 0L);
        long keyspaceHits = parseLong(statsProps, "keyspace_hits", 0L);
        long keyspaceMisses = parseLong(statsProps, "keyspace_misses", 0L);
        long instantaneousOpsPerSec = parseLong(statsProps, "instantaneous_ops_per_sec", 0L);
        long totalCommandsProcessed = parseLong(statsProps, "total_commands_processed", 0L);
        long uptimeInSeconds = parseLong(serverProps, "uptime_in_seconds", 0L);

        double hitRate = 0.0;
        long total = keyspaceHits + keyspaceMisses;
        if (total > 0) {
            hitRate = (double) keyspaceHits / total;
        }

        ServerMetrics metrics = new ServerMetrics(connectedClients, keyspaceHits,
                keyspaceMisses, hitRate, instantaneousOpsPerSec,
                totalCommandsProcessed, uptimeInSeconds);
        log.debug("Server metrics: {}", metrics);
        return metrics;
    }

    @Override
    public long getKeyCount() {
        Properties keyspaceProps = executeInfo("keyspace");

        long totalKeys = 0;
        // Keyspace entries look like: db0=keys=42,expires=5,avg_ttl=1234
        for (String name : keyspaceProps.stringPropertyNames()) {
            if (name.startsWith("db")) {
                String value = keyspaceProps.getProperty(name, "");
                totalKeys += parseKeysFromDbEntry(value);
            }
        }

        log.debug("Total key count: {}", totalKeys);
        return totalKeys;
    }

    // ---- Helper methods ----

    /**
     * Executes {@code INFO <section>} using the native connection API.
     */
    private Properties executeInfo(String section) {
        Properties props = stringRedisTemplate.execute(connection -> {
            return connection.serverCommands().info(section);
        }, true);
        return props != null ? props : new Properties();
    }

    /**
     * Safely parses a long value from Properties.
     */
    private long parseLong(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.trace("Cannot parse long from property '{}': '{}'", key, value);
            return defaultValue;
        }
    }

    /**
     * Parses the key count from a keyspace db entry string.
     * Format: {@code keys=42,expires=5,avg_ttl=1234}
     */
    private long parseKeysFromDbEntry(String dbEntry) {
        // Format: keys=42,expires=5,avg_ttl=1234
        for (String part : dbEntry.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("keys=")) {
                try {
                    return Long.parseLong(trimmed.substring("keys=".length()));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    /**
     * Parses the JSON array returned by the SLOWLOG GET Lua script.
     *
     * <p>Each entry in the JSON array is itself an array with the structure:
     * {@code [id, timestamp, duration, [cmd_arg1, cmd_arg2, ...], client_addr, client_name]}
     * where the command array (index 3) contains the command name followed by its arguments.</p>
     *
     * <p>This parser handles the nested JSON structure manually to avoid
     * requiring additional JSON library dependencies beyond what Spring provides.</p>
     */
    private List<SlowLogEntry> parseSlowLogJson(String json) {
        List<SlowLogEntry> entries = new ArrayList<>();

        try {
            // The JSON is an array of arrays: [[id,ts,dur,[cmd...],addr,...], ...]
            // We parse it in a simplified manner
            String trimmed = json.trim();
            if (!trimmed.startsWith("[")) {
                return entries;
            }

            // Remove outer brackets
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            if (trimmed.isEmpty()) {
                return entries;
            }

            // Split into individual entry arrays at the top level
            List<String> entryStrings = splitTopLevelArrays(trimmed);

            for (String entryStr : entryStrings) {
                try {
                    SlowLogEntry entry = parseSlowLogEntryArray(entryStr);
                    if (entry != null) {
                        entries.add(entry);
                    }
                } catch (Exception e) {
                    log.trace("Failed to parse slow log entry: {}", entryStr, e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse slow log JSON: {}", e.getMessage());
        }

        return entries;
    }

    /**
     * Splits a JSON string into top-level array elements, respecting nested brackets.
     */
    private List<String> splitTopLevelArrays(String content) {
        List<String> results = new ArrayList<>();
        int depth = 0;
        int start = -1;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '[') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0 && start >= 0) {
                    results.add(content.substring(start + 1, i));
                    start = -1;
                }
            }
        }
        return results;
    }

    /**
     * Parses a single slow log entry from its JSON array representation.
     * Expected format (inside brackets): {@code id, timestamp, duration, [cmd_args...], "client_addr", ...}
     */
    private SlowLogEntry parseSlowLogEntryArray(String entryContent) {
        // Find the nested command array first
        int cmdArrayStart = entryContent.indexOf('[');
        int cmdArrayEnd = entryContent.indexOf(']', cmdArrayStart);

        String beforeCmd = cmdArrayStart >= 0
                ? entryContent.substring(0, cmdArrayStart)
                : entryContent;
        String afterCmd = cmdArrayEnd >= 0 && cmdArrayEnd + 1 < entryContent.length()
                ? entryContent.substring(cmdArrayEnd + 1)
                : "";

        // Parse id, timestamp, duration from beforeCmd
        String[] beforeParts = beforeCmd.split(",");
        if (beforeParts.length < 3) {
            return null;
        }

        long id = parseJsonLong(beforeParts[0]);
        long timestamp = parseJsonLong(beforeParts[1]);
        long duration = parseJsonLong(beforeParts[2]);

        // Parse command from the nested array
        String command = "";
        if (cmdArrayStart >= 0 && cmdArrayEnd >= 0) {
            String cmdContent = entryContent.substring(cmdArrayStart + 1, cmdArrayEnd);
            command = parseCommandArray(cmdContent);
        }

        // Parse client address from afterCmd
        String clientAddress = "";
        String[] afterParts = afterCmd.split(",");
        for (String part : afterParts) {
            String stripped = part.trim().replaceAll("^\"|\"$", "");
            if (stripped.contains(":") && !stripped.isEmpty()) {
                clientAddress = stripped;
                break;
            }
        }

        return new SlowLogEntry(id, timestamp, duration, command, clientAddress);
    }

    /**
     * Parses a JSON number string to a long.
     */
    private long parseJsonLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Joins command array elements into a single command string.
     * Input: {@code "GET","mykey"} or {@code "SET","key","value"}
     */
    private String parseCommandArray(String cmdContent) {
        StringBuilder cmd = new StringBuilder();
        for (String part : cmdContent.split(",")) {
            String stripped = part.trim().replaceAll("^\"|\"$", "");
            if (!stripped.isEmpty()) {
                if (cmd.length() > 0) {
                    cmd.append(' ');
                }
                cmd.append(stripped);
            }
        }
        return cmd.toString();
    }
}
