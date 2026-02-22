package com.tutorial.redis.module08.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 RedisDataRecoveryAdapter 的資料寫入與恢復功能。
 * 驗證透過 Redis 進行資料寫入、計數及清除的持久化恢復操作。
 * 屬於 Adapter 層（外部介面卡），負責與 Redis 實際互動的整合測試。
 */
@DisplayName("RedisDataRecoveryAdapter 整合測試")
class RedisDataRecoveryAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisDataRecoveryAdapter adapter;

    // 驗證寫入指定數量的測試資料後，countKeys 能正確回傳寫入筆數
    @Test
    @DisplayName("writeTestData_AndCountKeys_ReturnsCorrectCount — 寫入 100 筆測試資料後計數，應回傳 100")
    void writeTestData_AndCountKeys_ReturnsCorrectCount() {
        // Arrange & Act
        adapter.writeTestData("recovery-test", 100);

        // Assert
        assertThat(adapter.countKeys("recovery-test")).isEqualTo(100);
    }

    // 驗證在無任何匹配 Key 的情況下，countKeys 回傳 0
    @Test
    @DisplayName("countKeys_WhenEmpty_ReturnsZero — 無資料時計數，應回傳 0")
    void countKeys_WhenEmpty_ReturnsZero() {
        // Act & Assert
        assertThat(adapter.countKeys("nonexistent-prefix")).isEqualTo(0);
    }

    // 驗證 flushAll 操作能清除所有已寫入的 Key
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
