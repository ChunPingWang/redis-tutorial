package com.tutorial.redis.module03.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StoreLocation 領域模型單元測試
 * 驗證門市位置物件的建構、經緯度驗證與空值檢查邏輯
 * 此模型用於 Geospatial 功能，屬於 Domain 層（領域模型）
 */
@DisplayName("StoreLocation 領域模型測試")
class StoreLocationTest {

    // 驗證以合法參數建構 StoreLocation 物件，各欄位值正確
    @Test
    @DisplayName("constructor_WhenValidArgs_CreatesLocation — 建立有效的門市位置")
    void constructor_WhenValidArgs_CreatesLocation() {
        StoreLocation location = new StoreLocation("STORE-001", "Taipei 101", 121.5654, 25.0330);

        assertThat(location.getStoreId()).isEqualTo("STORE-001");
        assertThat(location.getName()).isEqualTo("Taipei 101");
        assertThat(location.getLongitude()).isEqualTo(121.5654);
        assertThat(location.getLatitude()).isEqualTo(25.0330);
    }

    // 驗證經度超出有效範圍（-180~180）時拋出 IllegalArgumentException
    @Test
    @DisplayName("constructor_WhenInvalidLongitude_ThrowsIAE — 經度 200 拋出 IllegalArgumentException")
    void constructor_WhenInvalidLongitude_ThrowsIAE() {
        assertThatThrownBy(() -> new StoreLocation("STORE-001", "Invalid", 200, 25.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitude");
    }

    // 驗證 storeId 為 null 時拋出 NullPointerException
    @Test
    @DisplayName("constructor_WhenNullStoreId_ThrowsNPE — null storeId 拋出 NullPointerException")
    void constructor_WhenNullStoreId_ThrowsNPE() {
        assertThatThrownBy(() -> new StoreLocation(null, "Test", 121.0, 25.0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("storeId");
    }
}
