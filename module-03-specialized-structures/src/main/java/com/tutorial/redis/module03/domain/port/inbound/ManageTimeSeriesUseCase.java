package com.tutorial.redis.module03.domain.port.inbound;

import com.tutorial.redis.module03.domain.model.TimeSeriesDataPoint;

import java.util.List;

/**
 * Inbound port: manage time series data using Redis TimeSeries module.
 */
public interface ManageTimeSeriesUseCase {

    /**
     * Creates a new time series with the specified retention period.
     *
     * @param key         the time series key
     * @param retentionMs data retention period in milliseconds (0 = no retention)
     */
    void createTimeSeries(String key, long retentionMs);

    /**
     * Adds a data point to a time series.
     *
     * @param key       the time series key
     * @param timestamp epoch timestamp in milliseconds
     * @param value     the data point value
     */
    void addDataPoint(String key, long timestamp, double value);

    /**
     * Queries data points within a time range.
     *
     * @param key  the time series key
     * @param from start timestamp in milliseconds (inclusive)
     * @param to   end timestamp in milliseconds (inclusive)
     * @return list of data points within the range, ordered by timestamp ascending
     */
    List<TimeSeriesDataPoint> queryRange(String key, long from, long to);
}
