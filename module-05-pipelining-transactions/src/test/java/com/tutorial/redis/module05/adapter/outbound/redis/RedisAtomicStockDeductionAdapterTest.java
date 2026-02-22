package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module05.domain.model.StockDeductionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 原子庫存扣減 Adapter 整合測試 — 驗證透過 Lua Script 實現的原子性庫存扣減。
 * 展示 Redis Lua Script 技術：將「檢查庫存」與「扣減庫存」合併為原子操作，防止並發超賣。
 * 所屬層級：Adapter 層（outbound），負責與 Redis 的實際交互。
 */
@DisplayName("RedisAtomicStockDeductionAdapter 整合測試")
class RedisAtomicStockDeductionAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisAtomicStockDeductionAdapter adapter;

    // 驗證庫存充足時，Lua Script 扣減成功並正確更新剩餘庫存
    @Test
    @DisplayName("deductStock_WhenSufficientStock_ReturnsSuccess — 庫存充足時扣減成功")
    void deductStock_WhenSufficientStock_ReturnsSuccess() {
        // Arrange
        adapter.setStock("ITEM-001", 100);

        // Act
        StockDeductionResult result = adapter.deductStock("ITEM-001", 30);

        // Assert
        assertThat(result).isEqualTo(StockDeductionResult.SUCCESS);
        Optional<Long> remaining = adapter.getStock("ITEM-001");
        assertThat(remaining).isPresent();
        assertThat(remaining.get()).isEqualTo(70L);
    }

    // 驗證庫存不足時，Lua Script 拒絕扣減並回傳 INSUFFICIENT_STOCK，庫存維持不變
    @Test
    @DisplayName("deductStock_WhenInsufficientStock_ReturnsInsufficient — 庫存不足時回傳 INSUFFICIENT_STOCK 且庫存不變")
    void deductStock_WhenInsufficientStock_ReturnsInsufficient() {
        // Arrange
        adapter.setStock("ITEM-002", 10);

        // Act
        StockDeductionResult result = adapter.deductStock("ITEM-002", 50);

        // Assert
        assertThat(result).isEqualTo(StockDeductionResult.INSUFFICIENT_STOCK);
        Optional<Long> remaining = adapter.getStock("ITEM-002");
        assertThat(remaining).isPresent();
        assertThat(remaining.get()).isEqualTo(10L);
    }

    // 驗證商品 key 不存在時，回傳 KEY_NOT_FOUND 狀態
    @Test
    @DisplayName("deductStock_WhenKeyNotFound_ReturnsKeyNotFound — key 不存在時回傳 KEY_NOT_FOUND")
    void deductStock_WhenKeyNotFound_ReturnsKeyNotFound() {
        // Act — no stock initialized for this product
        StockDeductionResult result = adapter.deductStock("NON-EXISTENT", 5);

        // Assert
        assertThat(result).isEqualTo(StockDeductionResult.KEY_NOT_FOUND);
    }

    // 驗證 20 個執行緒同時扣減庫存時，Lua Script 的原子性確保不會超賣
    @Test
    @DisplayName("deductStock_ConcurrentDeductions_NeverOversells — 20 執行緒並發扣減不會超賣")
    void deductStock_ConcurrentDeductions_NeverOversells() throws InterruptedException {
        // Arrange — stock=100, 20 threads each deducting 10 (total demand=200)
        String productId = "CONCURRENT-001";
        adapter.setStock(productId, 100);

        int threadCount = 20;
        int deductionPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act — launch all threads simultaneously
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    StockDeductionResult result = adapter.deductStock(productId, deductionPerThread);
                    if (result == StockDeductionResult.SUCCESS) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads at once
        doneLatch.await();
        executor.shutdown();

        // Assert
        Optional<Long> remainingStock = adapter.getStock(productId);
        assertThat(remainingStock).isPresent();
        long remaining = remainingStock.get();

        // Stock must never go negative
        assertThat(remaining).isGreaterThanOrEqualTo(0L);

        // Exactly 10 should succeed (100 stock / 10 per thread = 10 successes)
        assertThat(successCount.get()).isEqualTo(10);

        // Invariant: successCount * deductionPerThread + remaining = initial stock
        assertThat(successCount.get() * deductionPerThread + remaining).isEqualTo(100L);
    }
}
