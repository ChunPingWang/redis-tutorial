package com.tutorial.redis.module04.domain.port.outbound;

import com.tutorial.redis.module04.domain.model.TransactionEvent;

import java.util.List;

/**
 * Outbound port for batch-persisting transaction events.
 * In the Write-Behind pattern, events drained from the Redis buffer
 * are saved to the underlying data store (database) through this port.
 * Implemented by a persistence adapter in the infrastructure layer.
 */
public interface TransactionPersistencePort {

    /**
     * Persists a batch of transaction events to the data store.
     *
     * @param events the events to save
     */
    void saveAll(List<TransactionEvent> events);
}
