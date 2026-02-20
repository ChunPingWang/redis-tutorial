package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisAccountCacheAdapter 整合測試")
class RedisAccountCacheAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisAccountCacheAdapter adapter;

    @Test
    @DisplayName("setAndGetBalance_ReturnsStoredBalance — 設定並取得餘額應回傳已儲存的值")
    void setAndGetBalance_ReturnsStoredBalance() {
        // Arrange
        String accountId = "acc-test-001";
        double balance = 12345.67;

        // Act
        adapter.setBalance(accountId, balance);
        Double result = adapter.getBalance(accountId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(balance);
    }

    @Test
    @DisplayName("storeAndGetProfile_ReturnsJsonProfile — 儲存並取得 JSON 檔案應回傳完整 profile")
    void storeAndGetProfile_ReturnsJsonProfile() {
        // Arrange
        String accountId = "acc-test-002";
        String jsonProfile = "{\"accountId\":\"acc-test-002\",\"ownerName\":\"Alice\","
                + "\"balance\":5000.0,\"currency\":\"USD\",\"createdAt\":1700000000}";

        // Act
        adapter.storeProfile(accountId, jsonProfile);
        String result = adapter.getProfile(accountId);

        // Assert — JSON.GET with '$' returns an array wrapper
        assertThat(result).isNotNull();
        assertThat(result).contains("acc-test-002");
        assertThat(result).contains("Alice");
        assertThat(result).contains("USD");
    }
}
