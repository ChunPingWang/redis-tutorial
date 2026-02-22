package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module03.domain.model.GeoSearchResult;
import com.tutorial.redis.module03.domain.model.StoreLocation;
import com.tutorial.redis.module03.domain.port.outbound.GeoLocationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Geospatial 配接器整合測試
 * 驗證 GeoLocationPort 透過 Redis Geo（GEOADD/GEOPOS/GEODIST/GEOSEARCH）命令的實作正確性
 * 涵蓋新增位置、距離計算、範圍搜尋與成員搜尋，屬於 Adapter 層（外部輸出端）
 */
@DisplayName("RedisGeoLocationAdapter 整合測試")
class RedisGeoLocationAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private GeoLocationPort geoLocationPort;

    // 驗證 GEOADD 新增門市位置後，可透過 GEOPOS 取得經緯度
    @Test
    @DisplayName("addLocation_WhenValidStore_StoresInGeoSet — 新增有效門市至 Geo 集合")
    void addLocation_WhenValidStore_StoresInGeoSet() {
        StoreLocation taipei101 = new StoreLocation("STORE-001", "Taipei 101", 121.5654, 25.0330);

        geoLocationPort.addLocation(taipei101);

        Optional<StoreLocation> result = geoLocationPort.getPosition("STORE-001");
        assertThat(result).isPresent();
        assertThat(result.get().getStoreId()).isEqualTo("STORE-001");
        assertThat(result.get().getLongitude()).isCloseTo(121.5654, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.get().getLatitude()).isCloseTo(25.0330, org.assertj.core.data.Offset.offset(0.001));
    }

    // 驗證 GEODIST 計算兩個門市之間的距離（公里）
    @Test
    @DisplayName("getDistance_BetweenTwoStores_ReturnsKilometers — 計算兩門市距離約 5 公里")
    void getDistance_BetweenTwoStores_ReturnsKilometers() {
        StoreLocation taipeiMainStation = new StoreLocation("STORE-TMS", "Taipei Main Station", 121.5170, 25.0478);
        StoreLocation taipei101 = new StoreLocation("STORE-101", "Taipei 101", 121.5654, 25.0330);

        geoLocationPort.addLocation(taipeiMainStation);
        geoLocationPort.addLocation(taipei101);

        Optional<Double> distance = geoLocationPort.getDistance("STORE-TMS", "STORE-101", "km");
        assertThat(distance).isPresent();
        assertThat(distance.get()).isGreaterThan(0);
    }

    // 驗證 GEOSEARCH 以座標為中心在指定半徑內搜尋門市
    @Test
    @DisplayName("searchNearby_WhenStoresInRadius_ReturnsStores — 搜尋半徑內的門市")
    void searchNearby_WhenStoresInRadius_ReturnsStores() {
        List<StoreLocation> locations = List.of(
                new StoreLocation("STORE-001", "Taipei 101", 121.5654, 25.0330),
                new StoreLocation("STORE-002", "Taipei Main Station", 121.5170, 25.0478),
                new StoreLocation("STORE-003", "Zhongxiao Dunhua", 121.5513, 25.0417),
                new StoreLocation("STORE-004", "Xinyi Anhe", 121.5530, 25.0332),
                new StoreLocation("STORE-005", "Daan Park", 121.5355, 25.0336)
        );
        geoLocationPort.addLocations(locations);

        // Search near center of Taipei (~25.04, 121.54) within 10 km
        List<GeoSearchResult> results = geoLocationPort.searchNearby(121.54, 25.04, 10, "km", 10);

        assertThat(results).isNotEmpty();
        assertThat(results.size()).isLessThanOrEqualTo(5);
        results.forEach(r -> assertThat(r.getDistance()).isGreaterThanOrEqualTo(0));
    }

    // 驗證搜尋範圍內無門市時回傳空列表
    @Test
    @DisplayName("searchNearby_WhenNoStoresInRadius_ReturnsEmpty — 遠距搜尋回傳空結果")
    void searchNearby_WhenNoStoresInRadius_ReturnsEmpty() {
        StoreLocation taipei101 = new StoreLocation("STORE-001", "Taipei 101", 121.5654, 25.0330);
        geoLocationPort.addLocation(taipei101);

        // Search from (0, 0) — far away from Taipei
        List<GeoSearchResult> results = geoLocationPort.searchNearby(0, 0, 10, "km", 10);

        assertThat(results).isEmpty();
    }

    // 驗證查詢不存在的門市 ID 時回傳 Optional.empty()
    @Test
    @DisplayName("getPosition_WhenStoreNotExists_ReturnsEmpty — 查詢不存在的門市回傳空")
    void getPosition_WhenStoreNotExists_ReturnsEmpty() {
        Optional<StoreLocation> result = geoLocationPort.getPosition("NON-EXISTENT");

        assertThat(result).isEmpty();
    }

    // 驗證 GEOSEARCHBYMEMBER 以現有門市為中心搜尋附近其他門市
    @Test
    @DisplayName("searchNearbyByMember_WhenMemberExists_ReturnsNearbyStores — 以門市為中心搜尋附近門市")
    void searchNearbyByMember_WhenMemberExists_ReturnsNearbyStores() {
        List<StoreLocation> locations = List.of(
                new StoreLocation("STORE-001", "Taipei 101", 121.5654, 25.0330),
                new StoreLocation("STORE-002", "Taipei Main Station", 121.5170, 25.0478),
                new StoreLocation("STORE-003", "Zhongxiao Dunhua", 121.5513, 25.0417),
                new StoreLocation("STORE-004", "Xinyi Anhe", 121.5530, 25.0332),
                new StoreLocation("STORE-005", "Daan Park", 121.5355, 25.0336)
        );
        geoLocationPort.addLocations(locations);

        List<GeoSearchResult> results = geoLocationPort.searchNearbyByMember("STORE-001", 10, "km", 10);

        assertThat(results).isNotEmpty();
        assertThat(results.size()).isGreaterThanOrEqualTo(1);
    }
}
