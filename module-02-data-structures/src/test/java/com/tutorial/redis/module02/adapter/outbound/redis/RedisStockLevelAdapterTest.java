package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module02.domain.port.outbound.StockLevelPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 庫存管理 Adapter 整合測試
 * 驗證使用 Redis String（SET/GET/INCRBY/DECRBY/MGET）管理商品庫存數量。
 * 層級：Adapter（外部端口實作）
 */
@DisplayName("RedisStockLevelAdapter 整合測試")
class RedisStockLevelAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private StockLevelPort stockLevelPort;

    // 驗證 SET 設定庫存數量後，GET 能正確讀取
    @Test
    @DisplayName("setLevel_WhenValidProduct_StoresLevel — 設定並讀取庫存數量")
    void setLevel_WhenValidProduct_StoresLevel() {
        stockLevelPort.setLevel("PROD-001", 100);

        OptionalLong level = stockLevelPort.getLevel("PROD-001");

        assertThat(level).isPresent();
        assertThat(level.getAsLong()).isEqualTo(100);
    }

    // 驗證 INCRBY 原子增量操作，回傳更新後的庫存值
    @Test
    @DisplayName("increment_WhenProductExists_ReturnsNewValue — 庫存增量後回傳新值")
    void increment_WhenProductExists_ReturnsNewValue() {
        stockLevelPort.setLevel("PROD-002", 10);

        long newValue = stockLevelPort.increment("PROD-002", 5);

        assertThat(newValue).isEqualTo(15);
        assertThat(stockLevelPort.getLevel("PROD-002").getAsLong()).isEqualTo(15);
    }

    // 驗證 DECRBY 原子減量操作，回傳更新後的庫存值
    @Test
    @DisplayName("decrement_WhenProductExists_ReturnsNewValue — 庫存減量後回傳新值")
    void decrement_WhenProductExists_ReturnsNewValue() {
        stockLevelPort.setLevel("PROD-003", 10);

        long newValue = stockLevelPort.decrement("PROD-003", 3);

        assertThat(newValue).isEqualTo(7);
        assertThat(stockLevelPort.getLevel("PROD-003").getAsLong()).isEqualTo(7);
    }

    // 驗證查詢不存在的 key 時回傳空值（OptionalLong.empty）
    @Test
    @DisplayName("getLevel_WhenNotExists_ReturnsEmpty — 查詢不存在的商品回傳空")
    void getLevel_WhenNotExists_ReturnsEmpty() {
        OptionalLong level = stockLevelPort.getLevel("NON-EXISTENT");

        assertThat(level).isEmpty();
    }

    // 驗證 MGET 一次查詢多個商品庫存，減少網路 RTT
    @Test
    @DisplayName("batchGetLevels_WhenMultipleProducts_ReturnsAll — 批次查詢多個商品庫存")
    void batchGetLevels_WhenMultipleProducts_ReturnsAll() {
        stockLevelPort.setLevel("PROD-A", 10);
        stockLevelPort.setLevel("PROD-B", 20);
        stockLevelPort.setLevel("PROD-C", 30);

        Map<String, Long> levels = stockLevelPort.batchGetLevels(List.of("PROD-A", "PROD-B", "PROD-C"));

        assertThat(levels).hasSize(3);
        assertThat(levels.get("PROD-A")).isEqualTo(10);
        assertThat(levels.get("PROD-B")).isEqualTo(20);
        assertThat(levels.get("PROD-C")).isEqualTo(30);
    }

    // 驗證 Redis key 遵循 ecommerce:stock:{productId} 命名規範
    @Test
    @DisplayName("key_FollowsNamingConvention — 驗證 key 符合 ecommerce:stock:* 命名規範")
    void key_FollowsNamingConvention() {
        stockLevelPort.setLevel("PROD-KEY-001", 50);

        Set<String> keys = stringRedisTemplate.keys("ecommerce:stock:PROD-KEY-001");
        assertThat(keys).isNotNull().hasSize(1);
        assertThat(keys.iterator().next()).isEqualTo("ecommerce:stock:PROD-KEY-001");
    }
}
