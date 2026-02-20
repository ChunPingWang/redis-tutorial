package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.port.inbound.ManageStockUseCase;
import com.tutorial.redis.module02.domain.port.outbound.StockLevelPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Application service implementing stock level management use cases.
 *
 * <p>Delegates to {@link StockLevelPort} for Redis String (atomic counter) operations.
 * Demonstrates Redis INCR/DECR for inventory management.</p>
 */
@Service
public class ManageStockService implements ManageStockUseCase {

    private final StockLevelPort stockLevelPort;

    public ManageStockService(StockLevelPort stockLevelPort) {
        this.stockLevelPort = stockLevelPort;
    }

    @Override
    public long restockProduct(String productId, long quantity) {
        return stockLevelPort.increment(productId, quantity);
    }

    @Override
    public long purchaseProduct(String productId, long quantity) {
        return stockLevelPort.decrement(productId, quantity);
    }

    @Override
    public OptionalLong getStockLevel(String productId) {
        return stockLevelPort.getLevel(productId);
    }

    @Override
    public Map<String, Long> getStockLevels(List<String> productIds) {
        return stockLevelPort.batchGetLevels(productIds);
    }
}
