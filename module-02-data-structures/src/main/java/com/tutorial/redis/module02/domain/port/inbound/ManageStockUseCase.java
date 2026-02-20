package com.tutorial.redis.module02.domain.port.inbound;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Inbound port: manage product stock levels using Redis String counters.
 */
public interface ManageStockUseCase {

    long restockProduct(String productId, long quantity);

    long purchaseProduct(String productId, long quantity);

    OptionalLong getStockLevel(String productId);

    Map<String, Long> getStockLevels(List<String> productIds);
}
