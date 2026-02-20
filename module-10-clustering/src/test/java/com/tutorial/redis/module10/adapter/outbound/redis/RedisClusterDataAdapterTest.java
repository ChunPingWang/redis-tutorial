package com.tutorial.redis.module10.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisClusterDataAdapter 整合測試")
class RedisClusterDataAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisClusterDataAdapter adapter;

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

    @Test
    @DisplayName("readData_WhenNotExists_ReturnsNull — 讀取不存在的 Key，應回傳 null")
    void readData_WhenNotExists_ReturnsNull() {
        // Act
        String result = adapter.readData("cluster:nonexistent-key");

        // Assert
        assertThat(result).isNull();
    }

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
