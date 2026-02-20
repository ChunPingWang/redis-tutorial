package com.tutorial.redis.module03.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeoService 領域服務測試")
class GeoServiceTest {

    private final GeoService geoService = new GeoService();

    @Test
    @DisplayName("isValidCoordinate_WhenValid_ReturnsTrue — 有效經緯度回傳 true")
    void isValidCoordinate_WhenValid_ReturnsTrue() {
        assertThat(geoService.isValidCoordinate(121.5654, 25.0330)).isTrue();
        assertThat(geoService.isValidCoordinate(-180, -90)).isTrue();
        assertThat(geoService.isValidCoordinate(180, 90)).isTrue();
        assertThat(geoService.isValidCoordinate(0, 0)).isTrue();
    }

    @Test
    @DisplayName("isValidCoordinate_WhenInvalidLongitude_ReturnsFalse — 經度 200 回傳 false")
    void isValidCoordinate_WhenInvalidLongitude_ReturnsFalse() {
        assertThat(geoService.isValidCoordinate(200, 25.0)).isFalse();
        assertThat(geoService.isValidCoordinate(-181, 25.0)).isFalse();
    }

    @Test
    @DisplayName("isValidCoordinate_WhenInvalidLatitude_ReturnsFalse — 緯度 100 回傳 false")
    void isValidCoordinate_WhenInvalidLatitude_ReturnsFalse() {
        assertThat(geoService.isValidCoordinate(121.0, 100)).isFalse();
        assertThat(geoService.isValidCoordinate(121.0, -91)).isFalse();
    }
}
