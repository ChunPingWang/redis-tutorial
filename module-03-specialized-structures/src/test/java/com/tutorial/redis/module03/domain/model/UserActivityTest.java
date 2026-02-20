package com.tutorial.redis.module03.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserActivity 領域模型測試")
class UserActivityTest {

    @Test
    @DisplayName("activityRate_WhenPartiallyActive_ReturnsCorrectRate — 15 天活躍 / 30 天 = 0.5")
    void activityRate_WhenPartiallyActive_ReturnsCorrectRate() {
        UserActivity activity = new UserActivity("USER-001", "202602", 15, 30);

        double rate = activity.activityRate();

        assertThat(rate).isEqualTo(0.5);
    }

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
