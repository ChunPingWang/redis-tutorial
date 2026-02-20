package com.tutorial.redis.module03.application.usecase;

import com.tutorial.redis.module03.domain.model.TimeSeriesDataPoint;
import com.tutorial.redis.module03.domain.port.inbound.ManageTimeSeriesUseCase;
import com.tutorial.redis.module03.domain.port.outbound.TimeSeriesPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Application service implementing time series management use cases.
 *
 * <p>Delegates to {@link TimeSeriesPort} for Redis TimeSeries module operations.
 * Demonstrates TS.CREATE, TS.ADD, and TS.RANGE for time-based data management.</p>
 */
@Service
public class ManageTimeSeriesService implements ManageTimeSeriesUseCase {

    private final TimeSeriesPort timeSeriesPort;

    public ManageTimeSeriesService(TimeSeriesPort timeSeriesPort) {
        this.timeSeriesPort = timeSeriesPort;
    }

    @Override
    public void createTimeSeries(String key, long retentionMs) {
        timeSeriesPort.create(key, retentionMs, Map.of());
    }

    @Override
    public void addDataPoint(String key, long timestamp, double value) {
        timeSeriesPort.add(key, timestamp, value);
    }

    @Override
    public List<TimeSeriesDataPoint> queryRange(String key, long from, long to) {
        return timeSeriesPort.range(key, from, to);
    }
}
