package com.tutorial.redis.module08.domain.model;

import java.util.Objects;

/**
 * Result of a data recovery test for a given persistence strategy.
 *
 * <p>After writing a known number of keys, simulating a restart, and counting
 * how many keys survived, this object captures:
 * <ul>
 *   <li>{@code strategy} — "rdb", "aof", or "hybrid"</li>
 *   <li>{@code keysWritten} — total keys written before the restart</li>
 *   <li>{@code keysRecovered} — keys found after the restart</li>
 *   <li>{@code recoveryTimeMs} — time in milliseconds for the recovery</li>
 *   <li>{@code dataLossPercentage} — {@code (1 - keysRecovered / keysWritten) * 100}</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class RecoveryResult {

    private String strategy;
    private int keysWritten;
    private int keysRecovered;
    private long recoveryTimeMs;
    private double dataLossPercentage;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public RecoveryResult() {
    }

    /**
     * Creates a RecoveryResult with the specified values.
     * The data loss percentage is calculated automatically.
     *
     * @param strategy       the persistence strategy tested ("rdb", "aof", "hybrid")
     * @param keysWritten    number of keys written before simulated restart
     * @param keysRecovered  number of keys found after recovery
     * @param recoveryTimeMs time taken for recovery in milliseconds
     */
    public RecoveryResult(String strategy, int keysWritten,
                          int keysRecovered, long recoveryTimeMs) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        this.keysWritten = keysWritten;
        this.keysRecovered = keysRecovered;
        this.recoveryTimeMs = recoveryTimeMs;
        this.dataLossPercentage = calculateDataLossPercentage(keysWritten, keysRecovered);
    }

    /**
     * Calculates the data loss percentage based on keys written and recovered.
     *
     * @param written   total keys written
     * @param recovered keys recovered after restart
     * @return percentage of data lost, between 0.0 and 100.0
     */
    private static double calculateDataLossPercentage(int written, int recovered) {
        if (written == 0) {
            return 0.0;
        }
        return (1.0 - (double) recovered / written) * 100.0;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public int getKeysWritten() {
        return keysWritten;
    }

    public void setKeysWritten(int keysWritten) {
        this.keysWritten = keysWritten;
        this.dataLossPercentage = calculateDataLossPercentage(keysWritten, this.keysRecovered);
    }

    public int getKeysRecovered() {
        return keysRecovered;
    }

    public void setKeysRecovered(int keysRecovered) {
        this.keysRecovered = keysRecovered;
        this.dataLossPercentage = calculateDataLossPercentage(this.keysWritten, keysRecovered);
    }

    public long getRecoveryTimeMs() {
        return recoveryTimeMs;
    }

    public void setRecoveryTimeMs(long recoveryTimeMs) {
        this.recoveryTimeMs = recoveryTimeMs;
    }

    public double getDataLossPercentage() {
        return dataLossPercentage;
    }

    public void setDataLossPercentage(double dataLossPercentage) {
        this.dataLossPercentage = dataLossPercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecoveryResult that)) return false;
        return keysWritten == that.keysWritten
                && keysRecovered == that.keysRecovered
                && recoveryTimeMs == that.recoveryTimeMs
                && Objects.equals(strategy, that.strategy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy, keysWritten, keysRecovered, recoveryTimeMs);
    }

    @Override
    public String toString() {
        return "RecoveryResult{strategy='%s', keysWritten=%d, keysRecovered=%d, recoveryTimeMs=%d, dataLossPercentage=%.2f%%}".formatted(
                strategy, keysWritten, keysRecovered, recoveryTimeMs, dataLossPercentage);
    }
}
