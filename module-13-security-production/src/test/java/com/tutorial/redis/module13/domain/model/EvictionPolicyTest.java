package com.tutorial.redis.module13.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 EvictionPolicy 列舉的正確性（Domain 層）。
 * 驗證 Redis 記憶體淘汰策略（noeviction、allkeys-lru、volatile-lru 等）
 * 的列舉值數量與 Redis 配置名稱對應是否正確。
 * 屬於六角形架構的 Domain Model 層。
 */
@DisplayName("EvictionPolicy 列舉測試")
class EvictionPolicyTest {

    // 驗證 EvictionPolicy 列舉定義了完整的 8 種 Redis 淘汰策略
    @Test
    @DisplayName("values_ReturnsEightPolicies — 應有 8 種淘汰策略")
    void values_ReturnsEightPolicies() {
        // Assert — Redis defines exactly 8 eviction policies
        assertThat(EvictionPolicy.values()).hasSize(8);
    }

    // 驗證 NOEVICTION 列舉值的 getRedisName() 回傳正確的 Redis 配置字串 "noeviction"
    @Test
    @DisplayName("noeviction_HasCorrectRedisName — NOEVICTION 的 Redis 名稱應為 noeviction")
    void noeviction_HasCorrectRedisName() {
        // Assert — NOEVICTION maps to the Redis config name "noeviction"
        assertThat(EvictionPolicy.NOEVICTION.getRedisName()).isEqualTo("noeviction");
    }
}
