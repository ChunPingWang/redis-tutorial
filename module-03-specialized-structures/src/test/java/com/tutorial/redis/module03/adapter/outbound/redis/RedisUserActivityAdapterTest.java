package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module03.domain.port.outbound.UserActivityPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisUserActivityAdapter 整合測試")
class RedisUserActivityAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private UserActivityPort userActivityPort;

    private static final String USER_ID = "USER-001";
    private static final String YEAR_MONTH = "202602";

    @Test
    @DisplayName("recordActivity_AndIsActive_ReturnsTrue — 記錄活躍日後查詢回傳 true")
    void recordActivity_AndIsActive_ReturnsTrue() {
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 1);

        boolean active = userActivityPort.isActive(USER_ID, YEAR_MONTH, 1);

        assertThat(active).isTrue();
    }

    @Test
    @DisplayName("isActive_WhenNotRecorded_ReturnsFalse — 未記錄的日期回傳 false")
    void isActive_WhenNotRecorded_ReturnsFalse() {
        boolean active = userActivityPort.isActive(USER_ID, YEAR_MONTH, 15);

        assertThat(active).isFalse();
    }

    @Test
    @DisplayName("countActiveDays_WhenMultipleDays_ReturnsCount — 記錄多日後計數正確")
    void countActiveDays_WhenMultipleDays_ReturnsCount() {
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 1);
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 3);
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 5);
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 7);

        long count = userActivityPort.countActiveDays(USER_ID, YEAR_MONTH);

        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("recordActivity_MultipleDays_AllActive — 記錄連續 10 天全部為活躍")
    void recordActivity_MultipleDays_AllActive() {
        for (int day = 1; day <= 10; day++) {
            userActivityPort.recordActivity(USER_ID, YEAR_MONTH, day);
        }

        for (int day = 1; day <= 10; day++) {
            assertThat(userActivityPort.isActive(USER_ID, YEAR_MONTH, day))
                    .as("Day %d should be active", day)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("countActiveDays_WhenNoDays_ReturnsZero — 無記錄時計數為零")
    void countActiveDays_WhenNoDays_ReturnsZero() {
        long count = userActivityPort.countActiveDays(USER_ID, YEAR_MONTH);

        assertThat(count).isZero();
    }
}
