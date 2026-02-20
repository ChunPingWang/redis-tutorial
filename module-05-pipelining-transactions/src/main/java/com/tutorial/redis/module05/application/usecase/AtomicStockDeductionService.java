package com.tutorial.redis.module05.application.usecase;

import com.tutorial.redis.module05.domain.model.StockDeductionResult;
import com.tutorial.redis.module05.domain.port.inbound.AtomicStockDeductionUseCase;
import com.tutorial.redis.module05.domain.port.outbound.AtomicStockDeductionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application service for atomic stock deduction operations using Redis Lua scripts.
 * Delegates to the {@link AtomicStockDeductionPort} outbound port which executes
 * a Lua script atomically on the Redis server.
 */
@Service
public class AtomicStockDeductionService implements AtomicStockDeductionUseCase {

    private static final Logger log = LoggerFactory.getLogger(AtomicStockDeductionService.class);

    private final AtomicStockDeductionPort atomicStockDeductionPort;

    public AtomicStockDeductionService(AtomicStockDeductionPort atomicStockDeductionPort) {
        this.atomicStockDeductionPort = atomicStockDeductionPort;
    }

    @Override
    public StockDeductionResult deductStock(String productId, int quantity) {
        log.debug("Deducting {} units of stock for product {}", quantity, productId);
        return atomicStockDeductionPort.deductStock(productId, quantity);
    }

    @Override
    public void initializeStock(String productId, long quantity) {
        log.debug("Initializing stock for product {}: {}", productId, quantity);
        atomicStockDeductionPort.setStock(productId, quantity);
    }

    @Override
    public Optional<Long> getStock(String productId) {
        log.debug("Getting stock for product {}", productId);
        return atomicStockDeductionPort.getStock(productId);
    }
}
