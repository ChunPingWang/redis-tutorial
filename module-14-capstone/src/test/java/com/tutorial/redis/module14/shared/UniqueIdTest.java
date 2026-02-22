package com.tutorial.redis.module14.shared;

import com.tutorial.redis.module14.shared.domain.model.UniqueId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UniqueId 領域模型單元測試類別。
 * 驗證唯一 ID 的格式化邏輯：前綴-時間戳-序號的組合格式。
 * 展示分散式唯一 ID 的領域模型設計，搭配 Redis INCR 產生序號。
 * 所屬：共用分散式模式 — shared 層（domain model）
 */
@DisplayName("UniqueId 領域模型單元測試")
class UniqueIdTest {

    // 驗證 ID 格式為「前綴-時間戳-六位數序號」
    @Test
    @DisplayName("toId_FormatsCorrectly — 產生的 ID 格式應正確")
    void toId_FormatsCorrectly() {
        // Arrange
        UniqueId uniqueId = new UniqueId("order", 42, 1700000000000L);

        // Act
        String id = uniqueId.toId();

        // Assert
        assertThat(id).isEqualTo("order-1700000000000-000042");
    }

    // 驗證序號不足六位數時，自動補零（例如 1 -> 000001）
    @Test
    @DisplayName("toId_PadsSequenceToSixDigits — 序號應補零至六位數")
    void toId_PadsSequenceToSixDigits() {
        // Arrange
        UniqueId uniqueId = new UniqueId("txn", 1, 1700000000000L);

        // Act
        String id = uniqueId.toId();

        // Assert
        assertThat(id).isEqualTo("txn-1700000000000-000001");
    }

    // 驗證序號超過六位數時不被截斷，完整保留
    @Test
    @DisplayName("toId_LargeSequence — 大序號不應被截斷")
    void toId_LargeSequence() {
        // Arrange
        UniqueId uniqueId = new UniqueId("evt", 1234567, 1700000000000L);

        // Act
        String id = uniqueId.toId();

        // Assert
        assertThat(id).isEqualTo("evt-1700000000000-1234567");
    }
}
