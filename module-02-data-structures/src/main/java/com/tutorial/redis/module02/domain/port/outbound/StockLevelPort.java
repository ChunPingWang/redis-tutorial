package com.tutorial.redis.module02.domain.port.outbound;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Outbound port for stock level operations.
 * Uses Redis String structure as an atomic counter.
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface StockLevelPort {

    void setLevel(String productId, long quantity);

    long increment(String productId, long delta);

    long decrement(String productId, long delta);

    OptionalLong getLevel(String productId);

    Map<String, Long> batchGetLevels(List<String> productIds);
}
