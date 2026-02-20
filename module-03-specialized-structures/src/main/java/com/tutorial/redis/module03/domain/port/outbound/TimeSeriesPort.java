package com.tutorial.redis.module03.domain.port.outbound;

import com.tutorial.redis.module03.domain.model.TimeSeriesDataPoint;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Outbound port for time series operations.
 * Uses Redis TimeSeries module commands (TS.CREATE / TS.ADD / TS.RANGE / TS.GET).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface TimeSeriesPort {

    /**
     * Creates a new time series key with retention and labels (TS.CREATE).
     *
     * @param key         the time series key
     * @param retentionMs data retention period in milliseconds (0 = no retention)
     * @param labels      metadata labels for the time series
     */
    void create(String key, long retentionMs, Map<String, String> labels);

    /**
     * Adds a data point with an explicit timestamp (TS.ADD).
     *
     * @param key       the time series key
     * @param timestamp epoch timestamp in milliseconds
     * @param value     the data point value
     */
    void add(String key, long timestamp, double value);

    /**
     * Adds a data point with an auto-generated timestamp (TS.ADD with * timestamp).
     *
     * @param key   the time series key
     * @param value the data point value
     */
    void addAutoTimestamp(String key, double value);

    /**
     * Retrieves data points within a time range (TS.RANGE).
     *
     * @param key           the time series key
     * @param fromTimestamp  start timestamp in milliseconds (inclusive)
     * @param toTimestamp    end timestamp in milliseconds (inclusive)
     * @return list of data points within the range, ordered by timestamp ascending
     */
    List<TimeSeriesDataPoint> range(String key, long fromTimestamp, long toTimestamp);

    /**
     * Retrieves the latest data point (TS.GET).
     *
     * @param key the time series key
     * @return the latest data point, or empty if the time series has no data
     */
    Optional<TimeSeriesDataPoint> getLatest(String key);
}
