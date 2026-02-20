package com.tutorial.redis.module07.domain.model;

import java.util.Objects;

/**
 * Represents a pending entry from XPENDING in a Redis Stream consumer group.
 * A pending message is one that has been delivered to a consumer but not yet
 * acknowledged via XACK.
 *
 * <p>Tracks the consumer name that owns the message, how long it has been
 * idle (unacknowledged), and how many times it has been delivered.
 * High delivery counts or long idle times may indicate a failed consumer
 * and trigger message claiming via XCLAIM.</p>
 *
 * Immutable value object — all fields are final.
 */
public class PendingMessage {

    private final String messageId;
    private final String consumerName;
    private final long idleTimeMs;
    private final long deliveryCount;

    public PendingMessage(String messageId, String consumerName,
                          long idleTimeMs, long deliveryCount) {
        this.messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        this.consumerName = Objects.requireNonNull(consumerName, "consumerName must not be null");
        this.idleTimeMs = idleTimeMs;
        this.deliveryCount = deliveryCount;
    }

    public String getMessageId() { return messageId; }
    public String getConsumerName() { return consumerName; }
    public long getIdleTimeMs() { return idleTimeMs; }
    public long getDeliveryCount() { return deliveryCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PendingMessage that)) return false;
        return messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "PendingMessage{messageId='%s', consumerName='%s', idleTimeMs=%d, deliveryCount=%d}".formatted(
                messageId, consumerName, idleTimeMs, deliveryCount);
    }
}
