package com.tutorial.redis.module03.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserActivity 領域模型單元測試
 * 驗證使用者活動紀錄物件的建構與活躍率計算邏輯
 * 此模型用於 Bitmap 活動追蹤功能，屬於 Domain 層（領域模型）
 */
@DisplayName("UserActivity 領域模型測試")
class UserActivityTest {

    // 驗證活躍率計算：15 天活躍 / 30 天總計 = 0.5（50%）
    @Test
    @DisplayName("activityRate_WhenPartiallyActive_ReturnsCorrectRate — 15 天活躍 / 30 天 = 0.5")
    void activityRate_WhenPartiallyActive_ReturnsCorrectRate() {
        UserActivity activity = new UserActivity("USER-001", "202602", 15, 30);

        double rate = activity.activityRate();

        assertThat(rate).isEqualTo(0.5);
    }

    // 驗證以合法參數建構 UserActivity 物件，各欄位值正確
    @Test
    @DisplayName("constructor_WhenValidArgs_CreatesActivity — 建立有效的使用者活動紀錄")
    void constructor_WhenValidArgs_CreatesActivity() {
        UserActivity activity = new UserActivity("USER-001", "202602", 20, 28);

        assertThat(activity.getUserId()).isEqualTo("USER-001");
        assertThat(activity.getYearMonth()).isEqualTo("202602");
        assertThat(activity.getActiveDays()).isEqualTo(20);
        assertThat(activity.getTotalDays()).isEqualTo(28);
    }
}
