package com.tutorial.redis.module13.application.usecase;

import com.tutorial.redis.module13.domain.model.MemoryInfo;
import com.tutorial.redis.module13.domain.model.ServerMetrics;
import com.tutorial.redis.module13.domain.model.SlowLogEntry;
import com.tutorial.redis.module13.domain.port.inbound.MonitoringUseCase;
import com.tutorial.redis.module13.domain.port.outbound.MonitoringPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing monitoring use cases.
 *
 * <p>Delegates all monitoring operations to the {@link MonitoringPort}
 * adapter, providing a clean boundary between the REST controller layer
 * and the Redis-specific implementation.</p>
 */
@Service
public class MonitoringService implements MonitoringUseCase {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final MonitoringPort monitoringPort;

    public MonitoringService(MonitoringPort monitoringPort) {
        this.monitoringPort = monitoringPort;
    }

    @Override
    public MemoryInfo getMemoryInfo() {
        log.info("Retrieving memory info");
        return monitoringPort.getMemoryInfo();
    }

    @Override
    public List<SlowLogEntry> getSlowLog(int count) {
        log.info("Retrieving slow log (count={})", count);
        return monitoringPort.getSlowLog(count);
    }

    @Override
    public ServerMetrics getServerMetrics() {
        log.info("Retrieving server metrics");
        return monitoringPort.getServerMetrics();
    }

    @Override
    public long getKeyCount() {
        log.info("Retrieving key count");
        return monitoringPort.getKeyCount();
    }
}
