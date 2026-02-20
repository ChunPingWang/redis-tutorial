package com.tutorial.redis.module08.application.usecase;

import com.tutorial.redis.module08.domain.model.PersistenceStatus;
import com.tutorial.redis.module08.domain.port.inbound.PersistenceInfoUseCase;
import com.tutorial.redis.module08.domain.port.outbound.PersistenceInfoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service for querying persistence status and triggering
 * persistence operations.
 *
 * <p>Implements the {@link PersistenceInfoUseCase} inbound port by delegating
 * to the {@link PersistenceInfoPort} outbound port. Acts as a thin orchestration
 * layer between the REST controller and the Redis adapter.</p>
 */
@Service
public class PersistenceInfoService implements PersistenceInfoUseCase {

    private static final Logger log = LoggerFactory.getLogger(PersistenceInfoService.class);

    private final PersistenceInfoPort persistenceInfoPort;

    public PersistenceInfoService(PersistenceInfoPort persistenceInfoPort) {
        this.persistenceInfoPort = persistenceInfoPort;
    }

    /**
     * Retrieves the current persistence status from the connected Redis instance.
     *
     * @return the current {@link PersistenceStatus}
     */
    @Override
    public PersistenceStatus getPersistenceStatus() {
        log.debug("Retrieving persistence status");
        return persistenceInfoPort.getPersistenceStatus();
    }

    /**
     * Triggers an RDB background save (BGSAVE).
     * Redis forks a child process to create a point-in-time snapshot.
     */
    @Override
    public void triggerRdbSnapshot() {
        log.info("Requesting RDB snapshot via BGSAVE");
        persistenceInfoPort.triggerBgsave();
    }

    /**
     * Triggers an AOF background rewrite (BGREWRITEAOF).
     * Redis forks a child process to compact the append-only file.
     */
    @Override
    public void triggerAofRewrite() {
        log.info("Requesting AOF rewrite via BGREWRITEAOF");
        persistenceInfoPort.triggerBgrewriteaof();
    }
}
