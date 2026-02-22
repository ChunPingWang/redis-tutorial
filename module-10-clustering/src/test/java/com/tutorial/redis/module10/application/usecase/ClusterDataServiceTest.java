package com.tutorial.redis.module10.application.usecase;

import com.tutorial.redis.module10.domain.port.outbound.ClusterDataPort;
import com.tutorial.redis.module10.domain.service.HashSlotCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * 測試 ClusterDataService 的 Hash Tag 寫入邏輯。
 * 驗證使用 Hash Tag 時，Key 是否正確組裝為 {tag}:subKey 格式，確保相關 Key 落在同一 Slot。
 * 屬於 Application 層（用例服務），透過 Mock 驗證業務邏輯的正確性。
 */
@DisplayName("ClusterDataService 單元測試")
@ExtendWith(MockitoExtension.class)
class ClusterDataServiceTest {

    @Mock
    private ClusterDataPort port;

    @Mock
    private HashSlotCalculator calculator;

    @InjectMocks
    private ClusterDataService service;

    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    // 驗證使用 Hash Tag 寫入時，Key 被正確組裝為 {tag}:subKey 格式以確保落在同一 Slot
    @Test
    @DisplayName("writeWithHashTag_BuildsCorrectKeys — 使用 Hash Tag 寫入時，Key 格式應為 {tag}:subKey")
    void writeWithHashTag_BuildsCorrectKeys() {
        // Arrange
        Map<String, String> subKeyValues = Map.of("cart", "items", "profile", "data");

        // Act
        service.writeWithHashTag("user:123", subKeyValues);

        // Assert — capture the Map passed to port.writeMultipleKeys
        verify(port).writeMultipleKeys(mapCaptor.capture());
        Map<String, String> capturedMap = mapCaptor.getValue();

        assertThat(capturedMap).hasSize(2);
        assertThat(capturedMap).containsKey("{user:123}:cart");
        assertThat(capturedMap).containsKey("{user:123}:profile");
        assertThat(capturedMap.get("{user:123}:cart")).isEqualTo("items");
        assertThat(capturedMap.get("{user:123}:profile")).isEqualTo("data");
    }
}
