package com.tutorial.redis.module09.domain.port.inbound;

import com.tutorial.redis.module09.domain.model.ReadWriteStrategy;
import com.tutorial.redis.module09.domain.model.ReplicationInfo;

import java.util.List;

/**
 * Inbound port: querying replication status and read-write splitting strategies.
 *
 * <p>Provides the application-level API for inspecting the current Redis
 * instance's replication configuration and for listing available
 * read-write splitting strategies in a master-replica topology.</p>
 */
public interface ReplicationInfoUseCase {

    /**
     * Retrieves the current replication information from the connected Redis instance.
     *
     * @return the current {@link ReplicationInfo}
     */
    ReplicationInfo getReplicationInfo();

    /**
     * Lists all available read-write splitting strategies with their descriptions.
     *
     * @return an unmodifiable list of all {@link ReadWriteStrategy} values
     */
    List<ReadWriteStrategy> listReadWriteStrategies();
}
