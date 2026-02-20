package com.tutorial.redis.module04.application.usecase;

import com.tutorial.redis.module04.adapter.outbound.redis.InMemoryProductCatalogRepository;
import com.tutorial.redis.module04.domain.model.ProductCatalog;
import com.tutorial.redis.module04.domain.port.inbound.ManageProductCatalogUseCase;
import com.tutorial.redis.module04.domain.port.outbound.ProductCatalogCachePort;
import com.tutorial.redis.module04.domain.port.outbound.ProductCatalogRepositoryPort;
import com.tutorial.redis.module04.domain.service.CacheTtlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application service implementing Read-Through / Write-Through caching
 * for product catalog management.
 *
 * <p>Read-Through: cache is checked first; on a miss the repository is queried
 * and the result is cached with a randomized TTL (anti-avalanche).</p>
 *
 * <p>Write-Through: writes go to both the repository and the cache.</p>
 */
@Service
public class ManageProductCatalogService implements ManageProductCatalogUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageProductCatalogService.class);
    private static final long BASE_TTL_MS = 30 * 60 * 1000L; // 30 minutes
    private static final double SPREAD_FACTOR = 0.3;

    private final ProductCatalogCachePort cachePort;
    private final ProductCatalogRepositoryPort repositoryPort;
    private final CacheTtlService cacheTtlService;

    public ManageProductCatalogService(ProductCatalogCachePort cachePort,
                                       ProductCatalogRepositoryPort repositoryPort,
                                       CacheTtlService cacheTtlService) {
        this.cachePort = cachePort;
        this.repositoryPort = repositoryPort;
        this.cacheTtlService = cacheTtlService;
    }

    @Override
    public Optional<ProductCatalog> getProduct(String productId) {
        // Step 1: Check cache
        Optional<ProductCatalog> cached = cachePort.findById(productId);
        if (cached.isPresent()) {
            log.debug("Cache HIT for product: {}", productId);
            return cached;
        }

        // Step 2: Cache miss — query repository (Read-Through)
        log.debug("Cache MISS for product: {}, querying repository", productId);
        Optional<ProductCatalog> fromRepo = repositoryPort.findById(productId);
        if (fromRepo.isEmpty()) {
            log.debug("Product not found in repository: {}", productId);
            return Optional.empty();
        }

        // Step 3: Save to cache with randomized TTL (anti-avalanche)
        ProductCatalog product = fromRepo.get();
        long ttl = cacheTtlService.randomizeTtl(BASE_TTL_MS, SPREAD_FACTOR);
        cachePort.save(product, ttl);
        log.debug("Cached product: {} with TTL: {}ms", productId, ttl);

        return fromRepo;
    }

    @Override
    public void saveProduct(ProductCatalog product) {
        // Write-Through: save to repository and cache simultaneously
        if (repositoryPort instanceof InMemoryProductCatalogRepository inMemoryRepo) {
            inMemoryRepo.addProduct(product);
        }
        long ttl = cacheTtlService.randomizeTtl(BASE_TTL_MS, SPREAD_FACTOR);
        cachePort.save(product, ttl);
        log.debug("Write-through: saved product {} to repository and cache", product.getProductId());
    }

    @Override
    public void evictProduct(String productId) {
        cachePort.evict(productId);
        log.debug("Evicted product from cache: {}", productId);
    }
}
