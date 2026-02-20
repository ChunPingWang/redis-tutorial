package com.tutorial.redis.module13.domain.model;

/**
 * Represents an entry from the Redis slow log.
 *
 * <p>Redis records commands that exceed the configured
 * {@code slowlog-log-slower-than} threshold (in microseconds) into an
 * in-memory slow log. Each entry captures:</p>
 * <ul>
 *   <li>{@code id} -- unique, auto-incrementing identifier</li>
 *   <li>{@code timestampSeconds} -- Unix timestamp when the command was logged</li>
 *   <li>{@code durationMicros} -- execution time in microseconds</li>
 *   <li>{@code command} -- the full command string (e.g. {@code "GET mykey"})</li>
 *   <li>{@code clientAddress} -- the client IP:port that issued the command</li>
 * </ul>
 *
 * <p>Populated by parsing the output of {@code SLOWLOG GET <count>}.</p>
 */
public class SlowLogEntry {

    private final long id;
    private final long timestampSeconds;
    private final long durationMicros;
    private final String command;
    private final String clientAddress;

    public SlowLogEntry(long id, long timestampSeconds, long durationMicros,
                        String command, String clientAddress) {
        this.id = id;
        this.timestampSeconds = timestampSeconds;
        this.durationMicros = durationMicros;
        this.command = command;
        this.clientAddress = clientAddress;
    }

    public long getId() {
        return id;
    }

    public long getTimestampSeconds() {
        return timestampSeconds;
    }

    public long getDurationMicros() {
        return durationMicros;
    }

    public String getCommand() {
        return command;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    @Override
    public String toString() {
        return "SlowLogEntry{id=" + id + ", timestampSeconds=" + timestampSeconds
                + ", durationMicros=" + durationMicros
                + ", command='" + command + '\''
                + ", clientAddress='" + clientAddress + "'}";
    }
}
