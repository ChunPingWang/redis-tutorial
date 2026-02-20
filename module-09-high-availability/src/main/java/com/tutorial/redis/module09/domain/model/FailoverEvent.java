package com.tutorial.redis.module09.domain.model;

import java.util.Objects;

/**
 * Simulated failover event for educational purposes.
 *
 * <p>Represents a single step in the Redis Sentinel failover process.
 * Each event captures what happened, a human-readable description,
 * and the timestamp when it occurred:
 * <ul>
 *   <li>{@code eventType} — the type of event:
 *       "SDOWN", "ODOWN", "FAILOVER_START", "FAILOVER_END", "NEW_MASTER"</li>
 *   <li>{@code description} — a detailed explanation of the event</li>
 *   <li>{@code timestampMs} — the event timestamp in epoch milliseconds</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class FailoverEvent {

    private String eventType;
    private String description;
    private long timestampMs;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public FailoverEvent() {
    }

    /**
     * Creates a FailoverEvent with the specified values.
     *
     * @param eventType   the type of failover event
     * @param description a human-readable explanation of the event
     * @param timestampMs the event timestamp in epoch milliseconds
     */
    public FailoverEvent(String eventType, String description, long timestampMs) {
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.timestampMs = timestampMs;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(long timestampMs) {
        this.timestampMs = timestampMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FailoverEvent that)) return false;
        return timestampMs == that.timestampMs
                && Objects.equals(eventType, that.eventType)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, description, timestampMs);
    }

    @Override
    public String toString() {
        return "FailoverEvent{eventType='%s', description='%s', timestampMs=%d}".formatted(
                eventType, description, timestampMs);
    }
}
