package com.tutorial.redis.module13.domain.model;

import java.util.List;

/**
 * Represents a Redis ACL (Access Control List) user.
 *
 * <p>Redis 6+ introduced a fine-grained ACL system where each user has:
 * <ul>
 *   <li>A <b>username</b> (the "default" user always exists)</li>
 *   <li>An <b>enabled/disabled</b> flag</li>
 *   <li>A set of allowed/denied <b>commands</b> (e.g. {@code +@all}, {@code -@dangerous})</li>
 *   <li>A set of allowed <b>key patterns</b> (e.g. {@code ~*}, {@code ~cache:*})</li>
 *   <li>A set of allowed <b>Pub/Sub channel patterns</b> (e.g. {@code &*})</li>
 * </ul>
 *
 * <p>This model is populated by parsing the output of {@code ACL LIST}.</p>
 */
public class AclUser {

    private final String username;
    private final boolean enabled;
    private final List<String> commands;
    private final List<String> keys;
    private final List<String> channels;

    public AclUser(String username, boolean enabled, List<String> commands,
                   List<String> keys, List<String> channels) {
        this.username = username;
        this.enabled = enabled;
        this.commands = commands;
        this.keys = keys;
        this.channels = channels;
    }

    public String getUsername() {
        return username;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getCommands() {
        return commands;
    }

    public List<String> getKeys() {
        return keys;
    }

    public List<String> getChannels() {
        return channels;
    }

    @Override
    public String toString() {
        return "AclUser{username='" + username + "', enabled=" + enabled
                + ", commands=" + commands + ", keys=" + keys
                + ", channels=" + channels + '}';
    }
}
