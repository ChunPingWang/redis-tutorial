package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisGeoAdapter 整合測試類別。
 * 驗證使用 Redis GEO 資料結構進行地理位置新增與附近搜尋功能。
 * 展示 GEOADD/GEORADIUS 在電商門市定位場景的應用。
 * 所屬：電商子系統 — adapter 層
 */
@DisplayName("RedisGeoAdapter 整合測試")
class RedisGeoAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisGeoAdapter adapter;

    // 驗證新增多個地理位置後，以指定座標搜尋附近門市僅回傳半徑範圍內的結果
    @Test
    @DisplayName("addAndFindNearby_ReturnsStoresWithinRadius — 新增位置後搜尋附近應回傳範圍內的門市")
    void addAndFindNearby_ReturnsStoresWithinRadius() {
        // Arrange — add stores around Taipei
        String key = "ecommerce:stores";
        // Taipei Main Station: 121.5170, 25.0478
        adapter.addLocation(key, 121.5170, 25.0478, "store-taipei-main");
        // Taipei 101: 121.5654, 25.0340
        adapter.addLocation(key, 121.5654, 25.0340, "store-101");
        // Taichung (far away): 120.6736, 24.1477
        adapter.addLocation(key, 120.6736, 24.1477, "store-taichung");

        // Act — search within 10 km of Taipei Main Station
        List<String> nearby = adapter.findNearby(key, 121.5170, 25.0478, 10.0);

        // Assert — should include Taipei stores but not Taichung
        assertThat(nearby).contains("store-taipei-main", "store-101");
        assertThat(nearby).doesNotContain("store-taichung");
    }
}
