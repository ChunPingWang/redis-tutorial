package com.tutorial.redis.module01.domain.port.inbound;

import com.tutorial.redis.module01.domain.model.Product;
import java.util.List;
import java.util.Optional;

/**
 * Inbound port: cache and retrieve products.
 */
public interface CacheProductUseCase {

    void cacheProduct(Product product);

    Optional<Product> getProduct(String productId);

    List<Product> getProducts(List<String> productIds);

    void evictProduct(String productId);
}
