package com.tutorial.redis.module04.application.usecase;

import com.tutorial.redis.module04.domain.model.CacheEntry;
import com.tutorial.redis.module04.domain.model.ProductCatalog;
import com.tutorial.redis.module04.domain.port.inbound.RefreshAheadCacheUseCase;
import com.tutorial.redis.module04.domain.port.outbound.ProductCatalogCachePort;
import com.tutorial.redis.module04.domain.port.outbound.ProductCatalogRepositoryPort;
import com.tutorial.redis.module04.domain.service.CacheTtlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application service implementing the Refresh-Ahead caching pattern
 * for product catalog data.
 *
 * <p>When a cached entry's remaining TTL falls below 20% of the original,
 * an asynchronous background refresh is triggered to reload the entry
 * before it expires, preventing cache misses for hot keys.</p>
 */
@Service
public class RefreshAheadCacheService implements RefreshAheadCacheUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshAheadCacheService.class);
    private static final long ORIGINAL_TTL_MS = 30 * 60 * 1000L; // 30 minutes

    private final ProductCatalogCachePort cachePort;
    private final ProductCatalogRepositoryPort repositoryPort;
    private final CacheTtlService cacheTtlService;

    /**
     * Tracks in-flight refresh operations to avoid duplicate async refreshes
     * for the same product.
     */
    private final Map<String, CacheEntry<ProductCatalog>> cacheEntries = new ConcurrentHashMap<>();

    public RefreshAheadCacheService(ProductCatalogCachePort cachePort,
                                    ProductCatalogRepositoryPort repositoryPort,
                                    CacheTtlService cacheTtlService) {
        this.cachePort = cachePort;
        this.repositoryPort = repositoryPort;
        this.cacheTtlService = cacheTtlService;
    }

    @Override
    public Optional<ProductCatalog> getWithRefreshAhead(String productId) {
        // Step 1: Check cache
        Optional<ProductCatalog> cached = cachePort.findById(productId);

        if (cached.isPresent()) {
            log.debug("Cache HIT for product: {}", productId);

            // Step 2: Check remaining TTL for refresh-ahead
            CacheEntry<ProductCatalog> entry = cacheEntries.get(productId);
            if (entry != null) {
                Instant now = Instant.now();
                long remainingTtl = entry.remainingTtlMs(now);

                if (cacheTtlService.shouldRefresh(remainingTtl, entry.getTtlMs())) {
                    log.debug("Triggering refresh-ahead for product: {} (remaining TTL: {}ms)",
                            productId, remainingTtl);
                    triggerAsyncRefresh(productId);
                }
            }

            return cached;
        }

        // Step 3: Cache miss — load from repository
        log.debug("Cache MISS for product: {}, loading from repository", productId);
        Optional<ProductCatalog> fromRepo = repositoryPort.findById(productId);
        if (fromRepo.isEmpty()) {
            return Optional.empty();
        }

        // Step 4: Save to cache and track the entry
        ProductCatalog product = fromRepo.get();
        saveToCache(productId, product);

        return fromRepo;
    }

    private void triggerAsyncRefresh(String productId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.debug("Async refresh started for product: {}", productId);
                Optional<ProductCatalog> refreshed = repositoryPort.findById(productId);
                refreshed.ifPresent(product -> saveToCache(productId, product));
                log.debug("Async refresh completed for product: {}", productId);
            } catch (Exception e) {
                log.warn("Async refresh failed for product: {}", productId, e);
            }
        });
    }

    private void saveToCache(String productId, ProductCatalog product) {
        cachePort.save(product, ORIGINAL_TTL_MS);
        CacheEntry<ProductCatalog> entry = new CacheEntry<>(product, Instant.now(), ORIGINAL_TTL_MS);
        cacheEntries.put(productId, entry);
    }
}
