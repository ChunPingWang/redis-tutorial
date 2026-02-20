package com.tutorial.redis.module04.domain.port.outbound;

import com.tutorial.redis.module04.domain.model.TransactionEvent;

import java.util.List;

/**
 * Outbound port for transaction event buffering.
 * Supports the Write-Behind (Write-Back) pattern where events are first
 * pushed to a Redis List as a temporary in-memory buffer and then
 * periodically drained in batches for persistence.
 * Implemented by a Redis adapter in the infrastructure layer.
 */
public interface TransactionBufferPort {

    /**
     * Appends a transaction event to the buffer.
     */
    void buffer(TransactionEvent event);

    /**
     * Removes and returns up to {@code batchSize} events from the buffer.
     * Events are drained in FIFO order.
     *
     * @param batchSize the maximum number of events to drain
     * @return the drained events (may be fewer than batchSize if the buffer is smaller)
     */
    List<TransactionEvent> drainBatch(int batchSize);

    /**
     * Returns the current number of events in the buffer.
     */
    long size();
}
