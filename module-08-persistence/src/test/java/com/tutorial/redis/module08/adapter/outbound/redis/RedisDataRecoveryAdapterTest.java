package com.tutorial.redis.module08.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisDataRecoveryAdapter 整合測試")
class RedisDataRecoveryAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisDataRecoveryAdapter adapter;

    @Test
    @DisplayName("writeTestData_AndCountKeys_ReturnsCorrectCount — 寫入 100 筆測試資料後計數，應回傳 100")
    void writeTestData_AndCountKeys_ReturnsCorrectCount() {
        // Arrange & Act
        adapter.writeTestData("recovery-test", 100);

        // Assert
        assertThat(adapter.countKeys("recovery-test")).isEqualTo(100);
    }

    @Test
    @DisplayName("countKeys_WhenEmpty_ReturnsZero — 無資料時計數，應回傳 0")
    void countKeys_WhenEmpty_ReturnsZero() {
        // Act & Assert
        assertThat(adapter.countKeys("nonexistent-prefix")).isEqualTo(0);
    }

    @Test
    @DisplayName("flushAll_ClearsAllData — flushAll 後所有 Key 應被清除")
    void flushAll_ClearsAllData() {
        // Arrange
        adapter.writeTestData("flush-test", 50);
        assertThat(adapter.countKeys("flush-test")).isEqualTo(50);

        // Act
        adapter.flushAll();

        // Assert
        assertThat(adapter.countKeys("flush-test")).isEqualTo(0);
    }
}
