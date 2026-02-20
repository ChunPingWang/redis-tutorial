package com.tutorial.redis.module10.domain.service;

import com.tutorial.redis.module10.domain.model.HashSlotInfo;
import com.tutorial.redis.module10.domain.model.HashTagAnalysis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HashSlotCalculator 領域服務測試")
class HashSlotCalculatorTest {

    private final HashSlotCalculator calculator = new HashSlotCalculator();

    @Test
    @DisplayName("calculateSlot_ReturnsValidRange — 任意 Key 的 Slot 應在 0~16383 範圍內")
    void calculateSlot_ReturnsValidRange() {
        // Act
        int slot = calculator.calculateSlot("test-key");

        // Assert
        assertThat(slot).isBetween(0, 16383);
    }

    @Test
    @DisplayName("calculateSlot_SameKey_ReturnsSameSlot — 相同 Key 應計算出相同的 Slot")
    void calculateSlot_SameKey_ReturnsSameSlot() {
        // Act
        int slot1 = calculator.calculateSlot("my-key");
        int slot2 = calculator.calculateSlot("my-key");

        // Assert
        assertThat(slot1).isEqualTo(slot2);
    }

    @Test
    @DisplayName("extractHashTag_WithBraces_ReturnsTag — {user:123}:cart 的 Hash Tag 應為 user:123")
    void extractHashTag_WithBraces_ReturnsTag() {
        // Act
        String tag = calculator.extractHashTag("{user:123}:cart");

        // Assert
        assertThat(tag).isEqualTo("user:123");
    }

    @Test
    @DisplayName("extractHashTag_WithoutBraces_ReturnsNull — 無大括號的 Key 應回傳 null")
    void extractHashTag_WithoutBraces_ReturnsNull() {
        // Act
        String tag = calculator.extractHashTag("user:123:cart");

        // Assert
        assertThat(tag).isNull();
    }

    @Test
    @DisplayName("extractHashTag_EmptyBraces_ReturnsNull — {} 空大括號應回傳 null")
    void extractHashTag_EmptyBraces_ReturnsNull() {
        // Act
        String tag = calculator.extractHashTag("{}:key");

        // Assert
        assertThat(tag).isNull();
    }

    @Test
    @DisplayName("analyzeHashTag_SameTag_SameSlot — 使用相同 Hash Tag 的 Key 應在同一 Slot")
    void analyzeHashTag_SameTag_SameSlot() {
        // Arrange
        List<String> keys = List.of("{user:123}:cart", "{user:123}:orders", "{user:123}:profile");

        // Act
        HashTagAnalysis result = calculator.analyzeHashTag(keys);

        // Assert
        assertThat(result.isSameSlot()).isTrue();
        assertThat(result.getSlot()).isBetween(0, 16383);
    }

    @Test
    @DisplayName("analyzeHashTag_DifferentKeys_MayDifferSlot — 不同 Key 可能在不同 Slot")
    void analyzeHashTag_DifferentKeys_MayDifferSlot() {
        // Arrange
        List<String> keys = List.of("user:1", "product:2", "order:3");

        // Act — verify the analysis runs without error
        HashTagAnalysis result = calculator.analyzeHashTag(keys);

        // Assert — the result is valid (either same or different slots)
        assertThat(result).isNotNull();
        assertThat(result.getKeys()).hasSize(3);
    }

    @Test
    @DisplayName("analyze_ReturnsFullInfo — analyze 應回傳完整的 HashSlotInfo")
    void analyze_ReturnsFullInfo() {
        // Act
        HashSlotInfo info = calculator.analyze("{tag}:key");

        // Assert
        assertThat(info.getKey()).isEqualTo("{tag}:key");
        assertThat(info.getHashTag()).isEqualTo("tag");
        assertThat(info.getSlot()).isBetween(0, 16383);
    }
}
