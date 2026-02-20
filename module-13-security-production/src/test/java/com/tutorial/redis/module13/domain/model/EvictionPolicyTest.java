package com.tutorial.redis.module13.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EvictionPolicy 列舉測試")
class EvictionPolicyTest {

    @Test
    @DisplayName("values_ReturnsEightPolicies — 應有 8 種淘汰策略")
    void values_ReturnsEightPolicies() {
        // Assert — Redis defines exactly 8 eviction policies
        assertThat(EvictionPolicy.values()).hasSize(8);
    }

    @Test
    @DisplayName("noeviction_HasCorrectRedisName — NOEVICTION 的 Redis 名稱應為 noeviction")
    void noeviction_HasCorrectRedisName() {
        // Assert — NOEVICTION maps to the Redis config name "noeviction"
        assertThat(EvictionPolicy.NOEVICTION.getRedisName()).isEqualTo("noeviction");
    }
}
