package com.tutorial.redis.module01.application.usecase;

import com.tutorial.redis.module01.domain.model.Product;
import com.tutorial.redis.module01.domain.port.inbound.CacheProductUseCase;
import com.tutorial.redis.module01.domain.port.outbound.ProductCachePort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CacheProductService implements CacheProductUseCase {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final ProductCachePort productCachePort;

    public CacheProductService(ProductCachePort productCachePort) {
        this.productCachePort = productCachePort;
    }

    @Override
    public void cacheProduct(Product product) {
        productCachePort.save(product, DEFAULT_TTL);
    }

    @Override
    public Optional<Product> getProduct(String productId) {
        return productCachePort.findById(productId);
    }

    @Override
    public List<Product> getProducts(List<String> productIds) {
        return productCachePort.findByIds(productIds);
    }

    @Override
    public void evictProduct(String productId) {
        productCachePort.evict(productId);
    }
}
