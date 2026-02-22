package com.tutorial.redis.module10.application.usecase;

import com.tutorial.redis.module10.domain.model.HashSlotInfo;
import com.tutorial.redis.module10.domain.model.HashTagAnalysis;
import com.tutorial.redis.module10.domain.service.HashSlotCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 測試 HashSlotService 的 Slot 計算與 Hash Tag 分析委派邏輯。
 * 驗證 Application 層是否正確將 Hash Slot 計算與 Hash Tag 分析委派給 Domain 層的 HashSlotCalculator。
 * 屬於 Application 層（用例服務），使用 Mockito 驗證方法委派行為。
 */
@DisplayName("HashSlotService 單元測試")
@ExtendWith(MockitoExtension.class)
class HashSlotServiceTest {

    @Mock
    private HashSlotCalculator calculator;

    @InjectMocks
    private HashSlotService service;

    // 驗證 calculateSlot 方法正確委派給 HashSlotCalculator 並回傳其結果
    @Test
    @DisplayName("calculateSlot_DelegatesToCalculator — 計算 Slot 應委派給 HashSlotCalculator")
    void calculateSlot_DelegatesToCalculator() {
        // Arrange
        HashSlotInfo expected = new HashSlotInfo("test-key", 12345, null);
        when(calculator.analyze("test-key")).thenReturn(expected);

        // Act
        HashSlotInfo result = service.calculateSlot("test-key");

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(calculator, times(1)).analyze("test-key");
    }

    // 驗證 analyzeHashTag 方法正確委派給 HashSlotCalculator 並回傳 Hash Tag 分析結果
    @Test
    @DisplayName("analyzeHashTag_DelegatesToCalculator — 分析 Hash Tag 應委派給 HashSlotCalculator")
    void analyzeHashTag_DelegatesToCalculator() {
        // Arrange
        List<String> keys = List.of("{user:1}:cart", "{user:1}:orders");
        HashTagAnalysis expected = new HashTagAnalysis(keys, "user:1", true, 5649);
        when(calculator.analyzeHashTag(keys)).thenReturn(expected);

        // Act
        HashTagAnalysis result = service.analyzeHashTag(keys);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(calculator, times(1)).analyzeHashTag(keys);
    }
}
