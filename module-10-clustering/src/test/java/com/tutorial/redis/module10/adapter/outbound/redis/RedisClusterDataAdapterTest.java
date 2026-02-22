package com.tutorial.redis.module10.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 RedisClusterDataAdapter 的 Redis 讀寫整合功能。
 * 驗證 Adapter 層對 Redis Cluster 的單筆與批次 Key-Value 操作是否正確。
 * 屬於 Adapter 層（外部介面卡），負責與 Redis 實際交互的整合測試。
 */
@DisplayName("RedisClusterDataAdapter 整合測試")
class RedisClusterDataAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisClusterDataAdapter adapter;

    // 驗證寫入單筆 Key-Value 後，讀取同一 Key 能回傳正確的值
    @Test
    @DisplayName("writeAndRead_ReturnsCorrectValue — 寫入後讀取，應回傳正確的值")
    void writeAndRead_ReturnsCorrectValue() {
        // Arrange
        adapter.writeData("cluster:key1", "value1");

        // Act
        String result = adapter.readData("cluster:key1");

        // Assert
        assertThat(result).isEqualTo("value1");
    }

    // 驗證讀取不存在的 Key 時，應回傳 null 而非拋出例外
    @Test
    @DisplayName("readData_WhenNotExists_ReturnsNull — 讀取不存在的 Key，應回傳 null")
    void readData_WhenNotExists_ReturnsNull() {
        // Act
        String result = adapter.readData("cluster:nonexistent-key");

        // Assert
        assertThat(result).isNull();
    }

    // 驗證批次寫入多筆 Key-Value 後，批次讀取能取回全部正確的值
    @Test
    @DisplayName("writeMultipleKeys_AndReadMultiple_ReturnsAll — 批次寫入 3 筆後批次讀取，應全部正確")
    void writeMultipleKeys_AndReadMultiple_ReturnsAll() {
        // Arrange
        Map<String, String> keyValues = Map.of("k1", "v1", "k2", "v2", "k3", "v3");
        adapter.writeMultipleKeys(keyValues);

        // Act
        Map<String, String> result = adapter.readMultipleKeys(List.of("k1", "k2", "k3"));

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get("k1")).isEqualTo("v1");
        assertThat(result.get("k2")).isEqualTo("v2");
        assertThat(result.get("k3")).isEqualTo("v3");
    }

    // 驗證批次讀取多筆不存在的 Key 時，每筆值應為 null
    @Test
    @DisplayName("readMultipleKeys_WhenEmpty_ReturnsNulls — 讀取不存在的多筆 Key，值應為 null")
    void readMultipleKeys_WhenEmpty_ReturnsNulls() {
        // Act
        Map<String, String> result = adapter.readMultipleKeys(
                List.of("nonexistent:1", "nonexistent:2", "nonexistent:3"));

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get("nonexistent:1")).isNull();
        assertThat(result.get("nonexistent:2")).isNull();
        assertThat(result.get("nonexistent:3")).isNull();
    }
}
