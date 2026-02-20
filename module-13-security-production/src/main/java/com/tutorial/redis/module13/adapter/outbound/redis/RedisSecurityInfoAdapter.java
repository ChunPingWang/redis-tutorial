package com.tutorial.redis.module13.adapter.outbound.redis;

import com.tutorial.redis.module13.domain.model.AclUser;
import com.tutorial.redis.module13.domain.port.outbound.SecurityInfoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Redis adapter for security information queries.
 *
 * <p>Implements {@link SecurityInfoPort} using {@link StringRedisTemplate} to
 * execute ACL commands via Lua scripts and retrieve INFO sections through
 * the native connection API.</p>
 *
 * <p>ACL LIST parsing: each entry returned by Redis looks like
 * {@code "user default on nopass ~* &* +@all"}. The adapter tokenizes
 * this string to extract the username, enabled flag, key patterns,
 * channel patterns, and command permissions.</p>
 */
@Component
public class RedisSecurityInfoAdapter implements SecurityInfoPort {

    private static final Logger log = LoggerFactory.getLogger(RedisSecurityInfoAdapter.class);

    /**
     * Lua script that returns ACL LIST as a JSON-encoded array of strings.
     * Each element is an ACL entry string like "user default on nopass ~* &* +@all".
     */
    private static final DefaultRedisScript<String> ACL_LIST_SCRIPT = new DefaultRedisScript<>(
            "return cjson.encode(redis.call('ACL', 'LIST'))",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisSecurityInfoAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<AclUser> listAclUsers() {
        try {
            String json = stringRedisTemplate.execute(ACL_LIST_SCRIPT, Collections.emptyList());
            if (json == null || json.isEmpty()) {
                log.warn("ACL LIST returned null/empty; returning default user");
                return List.of(createDefaultUser());
            }
            return parseAclListJson(json);
        } catch (Exception e) {
            log.warn("ACL LIST failed (possibly older Redis version): {}. Returning default user.", e.getMessage());
            return List.of(createDefaultUser());
        }
    }

    @Override
    public Map<String, String> getServerInfo(String section) {
        Properties props = stringRedisTemplate.execute(connection -> {
            return connection.serverCommands().info(section);
        }, true);

        if (props == null) {
            log.warn("INFO {} returned null", section);
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            result.put(name, props.getProperty(name));
        }
        log.debug("INFO {} returned {} properties", section, result.size());
        return result;
    }

    /**
     * Parses the JSON array returned by the ACL LIST Lua script.
     *
     * <p>The JSON looks like: {@code ["user default on nopass ~* &* +@all", "user admin on ..."]}
     * Each element is a space-separated ACL rule string.</p>
     */
    private List<AclUser> parseAclListJson(String json) {
        // Strip the outer brackets and split by comma
        // JSON format: ["entry1","entry2",...]
        List<AclUser> users = new ArrayList<>();

        // Simple JSON array parsing -- remove outer [ ] and split on ","
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        if (trimmed.isEmpty()) {
            return List.of(createDefaultUser());
        }

        // Split entries -- each entry is a quoted string
        List<String> entries = splitJsonStringArray(trimmed);

        for (String entry : entries) {
            try {
                AclUser user = parseAclEntry(entry);
                users.add(user);
            } catch (Exception e) {
                log.warn("Failed to parse ACL entry '{}': {}", entry, e.getMessage());
            }
        }

        return users.isEmpty() ? List.of(createDefaultUser()) : users;
    }

    /**
     * Splits a comma-separated list of JSON strings into individual unquoted values.
     */
    private List<String> splitJsonStringArray(String content) {
        List<String> results = new ArrayList<>();
        boolean inString = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (c == ',' && !inString) {
                results.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            results.add(current.toString().trim());
        }
        return results;
    }

    /**
     * Parses a single ACL entry string into an {@link AclUser}.
     *
     * <p>Format: {@code user <username> on|off [nopass|>password] [~pattern...] [&pattern...] [+cmd|-cmd...]}</p>
     */
    private AclUser parseAclEntry(String entry) {
        String[] tokens = entry.split("\\s+");

        String username = "unknown";
        boolean enabled = false;
        List<String> commands = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<String> channels = new ArrayList<>();

        int i = 0;
        // Skip "user" token if present
        if (i < tokens.length && "user".equalsIgnoreCase(tokens[i])) {
            i++;
        }
        // Username
        if (i < tokens.length) {
            username = tokens[i++];
        }

        // Parse remaining tokens
        for (; i < tokens.length; i++) {
            String token = tokens[i];
            if ("on".equalsIgnoreCase(token)) {
                enabled = true;
            } else if ("off".equalsIgnoreCase(token)) {
                enabled = false;
            } else if (token.startsWith("~")) {
                keys.add(token);
            } else if (token.startsWith("&")) {
                channels.add(token);
            } else if (token.startsWith("+") || token.startsWith("-")) {
                commands.add(token);
            }
            // Skip password tokens (nopass, >hash, #hash) and other flags
        }

        return new AclUser(username, enabled, commands, keys, channels);
    }

    /**
     * Creates a default user representation for environments where
     * ACL commands are not available (Redis < 6).
     */
    private AclUser createDefaultUser() {
        return new AclUser("default", true,
                List.of("+@all"), List.of("~*"), List.of("&*"));
    }
}
