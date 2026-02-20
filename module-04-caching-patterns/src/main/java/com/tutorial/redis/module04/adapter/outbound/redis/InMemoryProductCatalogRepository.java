package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.module04.domain.model.ProductCatalog;
import com.tutorial.redis.module04.domain.port.outbound.ProductCatalogRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory repository simulating a database for product catalog data.
 * Provides an {@link #addProduct(ProductCatalog)} method for test seeding.
 */
@Component
public class InMemoryProductCatalogRepository implements ProductCatalogRepositoryPort {

    private final ConcurrentMap<String, ProductCatalog> store = new ConcurrentHashMap<>();

    @Override
    public Optional<ProductCatalog> findById(String productId) {
        return Optional.ofNullable(store.get(productId));
    }

    /**
     * Adds a product to the in-memory store. Used for test seeding
     * and write-through persistence.
     *
     * @param product the product to store
     */
    public void addProduct(ProductCatalog product) {
        store.put(product.getProductId(), product);
    }
}
