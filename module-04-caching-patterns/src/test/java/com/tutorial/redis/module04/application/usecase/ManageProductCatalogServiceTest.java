package com.tutorial.redis.module04.application.usecase;

import com.tutorial.redis.module04.domain.model.ProductCatalog;
import com.tutorial.redis.module04.domain.port.outbound.ProductCatalogCachePort;
import com.tutorial.redis.module04.domain.port.outbound.ProductCatalogRepositoryPort;
import com.tutorial.redis.module04.domain.service.CacheTtlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageProductCatalogService 單元測試")
class ManageProductCatalogServiceTest {

    @Mock
    private ProductCatalogCachePort cachePort;

    @Mock
    private ProductCatalogRepositoryPort repositoryPort;

    @Mock
    private CacheTtlService cacheTtlService;

    @InjectMocks
    private ManageProductCatalogService service;

    private ProductCatalog createProduct(String id) {
        return new ProductCatalog(id, "Product " + id, "Electronics", 999.99, "Description");
    }

    @Test
    @DisplayName("getProduct_WhenCacheHit_ReturnsCached — 快取命中時直接回傳")
    void getProduct_WhenCacheHit_ReturnsCached() {
        ProductCatalog cached = createProduct("PROD-001");
        when(cachePort.findById("PROD-001")).thenReturn(Optional.of(cached));

        Optional<ProductCatalog> result = service.getProduct("PROD-001");

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("PROD-001");
        verify(cachePort).findById("PROD-001");
        verify(repositoryPort, never()).findById(anyString());
    }

    @Test
    @DisplayName("getProduct_WhenCacheMiss_QueriesRepoAndCaches — 快取未命中時查詢資料庫並寫入快取")
    void getProduct_WhenCacheMiss_QueriesRepoAndCaches() {
        ProductCatalog fromRepo = createProduct("PROD-002");
        when(cachePort.findById("PROD-002")).thenReturn(Optional.empty());
        when(repositoryPort.findById("PROD-002")).thenReturn(Optional.of(fromRepo));
        when(cacheTtlService.randomizeTtl(anyLong(), anyDouble())).thenReturn(1_800_000L);

        Optional<ProductCatalog> result = service.getProduct("PROD-002");

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("PROD-002");
        verify(cachePort).findById("PROD-002");
        verify(repositoryPort).findById("PROD-002");
        verify(cachePort).save(eq(fromRepo), eq(1_800_000L));
    }

    @Test
    @DisplayName("saveProduct_WritesToBothRepoAndCache — Write-Through 同時寫入資料庫與快取")
    void saveProduct_WritesToBothRepoAndCache() {
        ProductCatalog product = createProduct("PROD-003");
        when(cacheTtlService.randomizeTtl(anyLong(), anyDouble())).thenReturn(2_000_000L);

        service.saveProduct(product);

        verify(cachePort).save(eq(product), eq(2_000_000L));
    }

    @Test
    @DisplayName("evictProduct_EvictsFromCache — 驅逐操作委派至快取端口")
    void evictProduct_EvictsFromCache() {
        service.evictProduct("PROD-004");

        verify(cachePort).evict("PROD-004");
    }
}
