package com.tutorial.redis.module09.application.usecase;

import com.tutorial.redis.module09.domain.model.ReadWriteStrategy;
import com.tutorial.redis.module09.domain.model.ReplicationInfo;
import com.tutorial.redis.module09.domain.port.inbound.ReplicationInfoUseCase;
import com.tutorial.redis.module09.domain.port.outbound.ReplicationInfoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Application service for querying replication status and read-write
 * splitting strategies.
 *
 * <p>Implements the {@link ReplicationInfoUseCase} inbound port by delegating
 * to the {@link ReplicationInfoPort} outbound port. Acts as a thin orchestration
 * layer between the REST controller (inbound adapter) and the Redis adapter.</p>
 */
@Service
public class ReplicationInfoService implements ReplicationInfoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReplicationInfoService.class);

    private final ReplicationInfoPort replicationInfoPort;

    public ReplicationInfoService(ReplicationInfoPort replicationInfoPort) {
        this.replicationInfoPort = replicationInfoPort;
    }

    /**
     * Retrieves the current replication information from the connected Redis instance.
     *
     * @return the current {@link ReplicationInfo}
     */
    @Override
    public ReplicationInfo getReplicationInfo() {
        log.debug("Retrieving replication info");
        return replicationInfoPort.getReplicationInfo();
    }

    /**
     * Lists all available read-write splitting strategies.
     *
     * @return a list of all {@link ReadWriteStrategy} enum values
     */
    @Override
    public List<ReadWriteStrategy> listReadWriteStrategies() {
        log.debug("Listing all read-write splitting strategies");
        return Arrays.asList(ReadWriteStrategy.values());
    }
}
