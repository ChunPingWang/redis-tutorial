package com.tutorial.redis.module14.shared.domain.model;

/**
 * Represents a distributed unique identifier generated using Redis.
 *
 * <p>Combines a prefix, timestamp, and monotonically increasing sequence number
 * to produce globally unique, sortable identifiers without coordination.</p>
 */
public class UniqueId {

    private String prefix;
    private long sequence;
    private long timestamp;

    public UniqueId() {
    }

    public UniqueId(String prefix, long sequence, long timestamp) {
        this.prefix = prefix;
        this.sequence = sequence;
        this.timestamp = timestamp;
    }

    /**
     * Formats the unique ID as a composite string.
     *
     * @return the formatted ID in the pattern {@code prefix-timestamp-000001}
     */
    public String toId() {
        return prefix + "-" + timestamp + "-" + String.format("%06d", sequence);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "UniqueId{prefix='" + prefix + "', sequence=" + sequence
                + ", timestamp=" + timestamp + ", id='" + toId() + "'}";
    }
}
