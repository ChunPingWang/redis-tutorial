package com.tutorial.redis.module07.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a Redis Stream entry.
 * Maps to a single message in a Redis Stream produced via XADD
 * and consumed via XREAD / XREADGROUP / XRANGE.
 *
 * <p>Each entry has a unique message ID assigned by Redis (e.g. "1609459200000-0"),
 * belongs to a specific stream key, carries a payload of field-value pairs,
 * and records the timestamp when it was created.</p>
 *
 * Immutable value object — all fields are final, payload is defensively copied.
 */
public class StreamMessage {

    private final String messageId;
    private final String streamKey;
    private final Map<String, String> payload;
    private final Instant timestamp;

    public StreamMessage(String messageId, String streamKey,
                         Map<String, String> payload, Instant timestamp) {
        this.messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        this.streamKey = Objects.requireNonNull(streamKey, "streamKey must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        this.payload = Collections.unmodifiableMap(new HashMap<>(payload));
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public String getMessageId() { return messageId; }
    public String getStreamKey() { return streamKey; }
    public Map<String, String> getPayload() { return payload; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StreamMessage that)) return false;
        return messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "StreamMessage{messageId='%s', streamKey='%s', payload=%s, timestamp=%s}".formatted(
                messageId, streamKey, payload, timestamp);
    }
}
