package com.tutorial.redis.module03.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a time series — a named sequence of data points.
 * Container for {@link TimeSeriesDataPoint} entries.
 * Immutable value object — all fields are final, list is defensively copied.
 */
public class TimeSeries {

    private final String key;
    private final List<TimeSeriesDataPoint> dataPoints;

    public TimeSeries(String key, List<TimeSeriesDataPoint> dataPoints) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(dataPoints, "dataPoints must not be null");
        this.dataPoints = Collections.unmodifiableList(new ArrayList<>(dataPoints));
    }

    public String getKey() { return key; }

    public List<TimeSeriesDataPoint> getDataPoints() { return dataPoints; }

    public boolean isEmpty() {
        return dataPoints.isEmpty();
    }

    public int size() {
        return dataPoints.size();
    }

    /**
     * Returns the data point with the latest (highest) timestamp.
     */
    public Optional<TimeSeriesDataPoint> getLatest() {
        return dataPoints.stream()
                .max(Comparator.comparingLong(TimeSeriesDataPoint::getTimestamp));
    }

    /**
     * Returns the data point with the earliest (lowest) timestamp.
     */
    public Optional<TimeSeriesDataPoint> getEarliest() {
        return dataPoints.stream()
                .min(Comparator.comparingLong(TimeSeriesDataPoint::getTimestamp));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSeries that)) return false;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "TimeSeries{key='%s', size=%d}".formatted(key, dataPoints.size());
    }
}
