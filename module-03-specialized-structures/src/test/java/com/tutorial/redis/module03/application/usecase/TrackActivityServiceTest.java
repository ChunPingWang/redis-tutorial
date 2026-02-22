package com.tutorial.redis.module03.application.usecase;

import com.tutorial.redis.module03.domain.model.UserActivity;
import com.tutorial.redis.module03.domain.port.outbound.UserActivityPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 活動追蹤服務單元測試
 * 驗證 TrackActivityService 正確委派 Bitmap 相關操作至 UserActivityPort
 * 使用 Mockito 隔離外部依賴，屬於 Application 層（使用案例）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrackActivityService 單元測試")
class TrackActivityServiceTest {

    @Mock
    private UserActivityPort userActivityPort;

    @InjectMocks
    private TrackActivityService service;

    // 驗證記錄每日活動時正確委派至 UserActivityPort.recordActivity
    @Test
    @DisplayName("recordDailyActivity_DelegatesToPort — 委派至 Port 的 recordActivity 方法")
    void recordDailyActivity_DelegatesToPort() {
        service.recordDailyActivity("USER-001", "202602", 15);

        verify(userActivityPort).recordActivity("USER-001", "202602", 15);
    }

    // 驗證查詢某日是否活躍時正確委派至 UserActivityPort.isActive 並回傳結果
    @Test
    @DisplayName("wasActiveOnDay_DelegatesToPort — 委派至 Port 的 isActive 方法")
    void wasActiveOnDay_DelegatesToPort() {
        when(userActivityPort.isActive("USER-001", "202602", 15)).thenReturn(true);

        boolean result = service.wasActiveOnDay("USER-001", "202602", 15);

        assertThat(result).isTrue();
        verify(userActivityPort).isActive("USER-001", "202602", 15);
    }

    // 驗證取得月活動報告時正確建構 UserActivity 物件並計算活躍天數
    @Test
    @DisplayName("getMonthlyActivity_BuildsUserActivity — 建構 UserActivity 物件驗證活躍天數")
    void getMonthlyActivity_BuildsUserActivity() {
        when(userActivityPort.countActiveDays("USER-001", "202601")).thenReturn(20L);

        UserActivity activity = service.getMonthlyActivity("USER-001", "202601", 31);

        assertThat(activity.getUserId()).isEqualTo("USER-001");
        assertThat(activity.getYearMonth()).isEqualTo("202601");
        assertThat(activity.getActiveDays()).isEqualTo(20);
        assertThat(activity.getTotalDays()).isEqualTo(31);
        verify(userActivityPort).countActiveDays("USER-001", "202601");
    }
}
