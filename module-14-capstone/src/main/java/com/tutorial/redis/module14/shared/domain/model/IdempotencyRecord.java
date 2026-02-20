package com.tutorial.redis.module14.shared.domain.model;

/**
 * Represents an idempotency record for ensuring at-most-once processing.
 *
 * <p>Stores the result of a previously processed operation along with
 * creation timestamp and time-to-live for automatic expiration in Redis.</p>
 */
public class IdempotencyRecord {

    private String idempotencyKey;
    private String result;
    private long createdAt;
    private long ttlSeconds;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String result, long createdAt, long ttlSeconds) {
        this.idempotencyKey = idempotencyKey;
        this.result = result;
        this.createdAt = createdAt;
        this.ttlSeconds = ttlSeconds;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String toString() {
        return "IdempotencyRecord{idempotencyKey='" + idempotencyKey
                + "', result='" + result + "', createdAt=" + createdAt
                + ", ttlSeconds=" + ttlSeconds + '}';
    }
}
