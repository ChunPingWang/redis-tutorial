package com.tutorial.redis.module14.shared;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module14.shared.adapter.outbound.redis.RedisIdempotencyAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisIdempotencyAdapter 整合測試類別。
 * 驗證使用 Redis SET NX EX 實作冪等性保證的功能。
 * 展示 Redis 原子性 SET NX 搭配 TTL 防止重複操作的技術。
 * 所屬：共用分散式模式 — shared 層
 */
@DisplayName("RedisIdempotencyAdapter 整合測試")
class RedisIdempotencyAdapterTest extends AbstractRedisIntegrationTest {

    private RedisIdempotencyAdapter idempotencyAdapter;

    @BeforeEach
    void setUpAdapter() {
        idempotencyAdapter = new RedisIdempotencyAdapter(stringRedisTemplate);
    }

    // 驗證首次設定冪等鍵時應回傳 true（表示設定成功）
    @Test
    @DisplayName("setIfAbsent_FirstTime_ReturnsTrue — 首次設定應回傳 true")
    void setIfAbsent_FirstTime_ReturnsTrue() {
        // Act
        boolean result = idempotencyAdapter.setIfAbsent("idem-key-1", "result-1", 60);

        // Assert
        assertThat(result).isTrue();
    }

    // 驗證重複設定同一冪等鍵時應回傳 false（表示已存在，防止重複操作）
    @Test
    @DisplayName("setIfAbsent_SecondTime_ReturnsFalse — 重複設定應回傳 false")
    void setIfAbsent_SecondTime_ReturnsFalse() {
        // Arrange
        idempotencyAdapter.setIfAbsent("idem-key-2", "result-2", 60);

        // Act
        boolean result = idempotencyAdapter.setIfAbsent("idem-key-2", "result-2-dup", 60);

        // Assert
        assertThat(result).isFalse();
    }

    // 驗證設定冪等鍵後，取得應回傳先前儲存的結果值
    @Test
    @DisplayName("get_AfterSet_ReturnsStoredValue — 設定後取得應回傳儲存的值")
    void get_AfterSet_ReturnsStoredValue() {
        // Arrange
        idempotencyAdapter.setIfAbsent("idem-key-3", "my-result", 60);

        // Act
        String value = idempotencyAdapter.get("idem-key-3");

        // Assert
        assertThat(value).isEqualTo("my-result");
    }

    // 驗證取得不存在的冪等鍵時應回傳 null
    @Test
    @DisplayName("get_NonExistentKey_ReturnsNull — 不存在的 Key 應回傳 null")
    void get_NonExistentKey_ReturnsNull() {
        // Act
        String value = idempotencyAdapter.get("non-existent-key");

        // Assert
        assertThat(value).isNull();
    }
}
