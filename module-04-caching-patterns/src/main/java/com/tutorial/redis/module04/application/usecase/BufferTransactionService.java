package com.tutorial.redis.module04.application.usecase;

import com.tutorial.redis.module04.domain.model.TransactionEvent;
import com.tutorial.redis.module04.domain.port.inbound.BufferTransactionUseCase;
import com.tutorial.redis.module04.domain.port.outbound.TransactionBufferPort;
import com.tutorial.redis.module04.domain.port.outbound.TransactionPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing the Write-Behind (Write-Back) pattern
 * for transaction events.
 *
 * <p>Events are first buffered in a fast Redis List, then periodically
 * drained in batches and persisted to the underlying data store.</p>
 */
@Service
public class BufferTransactionService implements BufferTransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(BufferTransactionService.class);

    private final TransactionBufferPort bufferPort;
    private final TransactionPersistencePort persistencePort;

    public BufferTransactionService(TransactionBufferPort bufferPort,
                                    TransactionPersistencePort persistencePort) {
        this.bufferPort = bufferPort;
        this.persistencePort = persistencePort;
    }

    @Override
    public void bufferTransaction(TransactionEvent event) {
        bufferPort.buffer(event);
        log.debug("Buffered transaction: {}", event.getTransactionId());
    }

    @Override
    public int flushBuffer(int batchSize) {
        // Drain events from the buffer
        List<TransactionEvent> batch = bufferPort.drainBatch(batchSize);
        if (batch.isEmpty()) {
            log.debug("No transactions to flush");
            return 0;
        }

        // Persist the batch to the data store
        persistencePort.saveAll(batch);
        log.debug("Flushed {} transactions to persistent store", batch.size());

        return batch.size();
    }
}
