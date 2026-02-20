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

@DisplayName("RedisAtomicStockDeductionAdapter 整合測試")
class RedisAtomicStockDeductionAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisAtomicStockDeductionAdapter adapter;

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

    @Test
    @DisplayName("deductStock_WhenKeyNotFound_ReturnsKeyNotFound — key 不存在時回傳 KEY_NOT_FOUND")
    void deductStock_WhenKeyNotFound_ReturnsKeyNotFound() {
        // Act — no stock initialized for this product
        StockDeductionResult result = adapter.deductStock("NON-EXISTENT", 5);

        // Assert
        assertThat(result).isEqualTo(StockDeductionResult.KEY_NOT_FOUND);
    }

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
