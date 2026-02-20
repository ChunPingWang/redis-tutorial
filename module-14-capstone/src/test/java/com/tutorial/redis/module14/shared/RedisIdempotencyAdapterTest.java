package com.tutorial.redis.module14.shared;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module14.shared.adapter.outbound.redis.RedisIdempotencyAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisIdempotencyAdapter 整合測試")
class RedisIdempotencyAdapterTest extends AbstractRedisIntegrationTest {

    private RedisIdempotencyAdapter idempotencyAdapter;

    @BeforeEach
    void setUpAdapter() {
        idempotencyAdapter = new RedisIdempotencyAdapter(stringRedisTemplate);
    }

    @Test
    @DisplayName("setIfAbsent_FirstTime_ReturnsTrue — 首次設定應回傳 true")
    void setIfAbsent_FirstTime_ReturnsTrue() {
        // Act
        boolean result = idempotencyAdapter.setIfAbsent("idem-key-1", "result-1", 60);

        // Assert
        assertThat(result).isTrue();
    }

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

    @Test
    @DisplayName("get_NonExistentKey_ReturnsNull — 不存在的 Key 應回傳 null")
    void get_NonExistentKey_ReturnsNull() {
        // Act
        String value = idempotencyAdapter.get("non-existent-key");

        // Assert
        assertThat(value).isNull();
    }
}
