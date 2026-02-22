package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisVisitorCountAdapter 整合測試類別。
 * 驗證使用 Redis HyperLogLog 統計不重複訪客數量的功能。
 * 展示 PFADD/PFCOUNT 在電商頁面流量統計場景的應用。
 * 所屬：電商子系統 — adapter 層
 */
@DisplayName("RedisVisitorCountAdapter 整合測試")
class RedisVisitorCountAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisVisitorCountAdapter adapter;

    // 驗證記錄多位訪客（含重複）後，HyperLogLog 正確回傳不重複訪客數
    @Test
    @DisplayName("recordAndCount_ReturnsUniqueCount — 記錄訪客後應回傳不重複訪客數")
    void recordAndCount_ReturnsUniqueCount() {
        // Arrange — record visits from 3 unique visitors (one visits twice)
        String pageId = "home";
        adapter.recordVisit(pageId, "visitor-1");
        adapter.recordVisit(pageId, "visitor-2");
        adapter.recordVisit(pageId, "visitor-3");
        adapter.recordVisit(pageId, "visitor-1"); // duplicate

        // Act
        long uniqueCount = adapter.getUniqueVisitorCount(pageId);

        // Assert — HyperLogLog should report approximately 3 unique visitors
        assertThat(uniqueCount).isEqualTo(3);
    }
}
