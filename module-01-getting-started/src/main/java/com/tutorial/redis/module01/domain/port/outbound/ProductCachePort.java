package com.tutorial.redis.module01.domain.port.outbound;

import com.tutorial.redis.module01.domain.model.Product;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Product cache operations.
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface ProductCachePort {

    void save(Product product, Duration ttl);

    Optional<Product> findById(String productId);

    void evict(String productId);

    List<Product> findByIds(List<String> productIds);
}
