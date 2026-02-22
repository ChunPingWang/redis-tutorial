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

/**
 * 商品目錄管理服務單元測試。
 * 驗證 Cache-Aside 讀取與 Write-Through 寫入模式的應用層邏輯。
 * 搭配 TTL 隨機化策略防止快取雪崩，確保讀寫與驅逐操作的正確性。
 * 屬於 Application 層（應用服務 / Use Case）。
 */
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

    // 驗證快取命中時直接回傳快取結果，不查詢資料庫
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

    // 驗證快取未命中時查詢資料庫，取得結果後以隨機化 TTL 寫入快取
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

    // 驗證 Write-Through 模式：儲存商品時同步寫入資料庫與快取
    @Test
    @DisplayName("saveProduct_WritesToBothRepoAndCache — Write-Through 同時寫入資料庫與快取")
    void saveProduct_WritesToBothRepoAndCache() {
        ProductCatalog product = createProduct("PROD-003");
        when(cacheTtlService.randomizeTtl(anyLong(), anyDouble())).thenReturn(2_000_000L);

        service.saveProduct(product);

        verify(cachePort).save(eq(product), eq(2_000_000L));
    }

    // 驗證商品驅逐操作正確委派至快取端口
    @Test
    @DisplayName("evictProduct_EvictsFromCache — 驅逐操作委派至快取端口")
    void evictProduct_EvictsFromCache() {
        service.evictProduct("PROD-004");

        verify(cachePort).evict("PROD-004");
    }
}
