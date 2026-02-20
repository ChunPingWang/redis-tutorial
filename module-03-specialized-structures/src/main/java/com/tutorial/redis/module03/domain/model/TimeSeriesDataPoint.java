package com.tutorial.redis.module03.domain.model;

import java.util.Objects;

/**
 * Represents a single data point in a time series.
 * Maps to Redis TimeSeries structure (TS.ADD / TS.RANGE / TS.GET).
 * Immutable value object — all fields are final.
 */
public class TimeSeriesDataPoint {

    private final long timestamp;
    private final double value;

    public TimeSeriesDataPoint(long timestamp, double value) {
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp must be greater than 0, got: " + timestamp);
        }
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() { return timestamp; }
    public double getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSeriesDataPoint that)) return false;
        return timestamp == that.timestamp && Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value);
    }

    @Override
    public String toString() {
        return "TimeSeriesDataPoint{timestamp=%d, value=%.4f}".formatted(timestamp, value);
    }
}
