package com.tutorial.redis.module04.domain.port.inbound;

import com.tutorial.redis.module04.domain.model.TransactionEvent;

/**
 * Inbound port: buffer and flush transaction events using the
 * Write-Behind (Write-Back) pattern.
 * Events are first written to a fast in-memory buffer (Redis List)
 * and then periodically flushed in batches to the persistent store.
 */
public interface BufferTransactionUseCase {

    /**
     * Buffers a transaction event for later batch persistence.
     *
     * @param event the transaction event to buffer
     */
    void bufferTransaction(TransactionEvent event);

    /**
     * Drains up to {@code batchSize} events from the buffer and persists
     * them to the data store.
     *
     * @param batchSize the maximum number of events to flush
     * @return the actual number of events flushed
     */
    int flushBuffer(int batchSize);
}
