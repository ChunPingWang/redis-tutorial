package com.tutorial.redis.module07.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an account domain event for event sourcing.
 * Each event is stored as a Redis Stream entry and can be replayed
 * to reconstruct the current {@link AccountState}.
 *
 * <p>Supported event types:</p>
 * <ul>
 *   <li>{@code ACCOUNT_OPENED} — creates the account (amount may be null)</li>
 *   <li>{@code MONEY_DEPOSITED} — adds amount to the balance</li>
 *   <li>{@code MONEY_WITHDRAWN} — subtracts amount from the balance</li>
 *   <li>{@code ACCOUNT_FROZEN} — freezes the account (amount may be null)</li>
 * </ul>
 *
 * Immutable value object — all fields are final, metadata is defensively copied.
 */
public class AccountEvent {

    private final String eventId;
    private final String accountId;
    private final String eventType;
    private final Double amount;
    private final Instant timestamp;
    private final Map<String, String> metadata;

    public AccountEvent(String eventId, String accountId, String eventType,
                        Double amount, Instant timestamp, Map<String, String> metadata) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.amount = amount;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    public String getEventId() { return eventId; }
    public String getAccountId() { return accountId; }
    public String getEventType() { return eventType; }
    public Double getAmount() { return amount; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, String> getMetadata() { return metadata; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountEvent that)) return false;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "AccountEvent{eventId='%s', accountId='%s', eventType='%s', amount=%s, timestamp=%s}".formatted(
                eventId, accountId, eventType, amount, timestamp);
    }
}
