package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisVisitorCountAdapter 整合測試")
class RedisVisitorCountAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisVisitorCountAdapter adapter;

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
