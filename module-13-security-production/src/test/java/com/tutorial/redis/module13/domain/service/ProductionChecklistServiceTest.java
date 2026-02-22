package com.tutorial.redis.module13.domain.service;

import com.tutorial.redis.module13.domain.model.ProductionCheckItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 ProductionChecklistService 領域服務（Domain 層）。
 * 驗證 Redis 正式環境上線檢查清單的產生邏輯，確保涵蓋
 * Security、Memory、Persistence、High Availability、Monitoring 五大類別。
 * 屬於六角形架構的 Domain Service 層。
 */
@DisplayName("ProductionChecklistService 領域服務測試")
class ProductionChecklistServiceTest {

    private final ProductionChecklistService service = new ProductionChecklistService();

    // 驗證產生的上線檢查清單涵蓋 Security、Memory、Persistence、HA、Monitoring 五大類別
    @Test
    @DisplayName("getChecklist_ReturnsAllCategories — 產生檢查清單應涵蓋所有類別")
    void getChecklist_ReturnsAllCategories() {
        // Act
        List<ProductionCheckItem> checklist = service.getChecklist();

        // Assert — checklist should not be empty and must cover all main categories
        assertThat(checklist).isNotEmpty();

        List<String> categories = checklist.stream()
                .map(ProductionCheckItem::getCategory)
                .distinct()
                .toList();
        assertThat(categories).contains("Security", "Memory", "Persistence",
                "High Availability", "Monitoring");
    }

    // 驗證 Security 類別至少包含 3 個檢查項目（如密碼設定、ACL、網路安全等）
    @Test
    @DisplayName("getChecklist_HasSecurityItems — Security 類別應至少有 3 個檢查項")
    void getChecklist_HasSecurityItems() {
        // Act
        List<ProductionCheckItem> checklist = service.getChecklist();

        // Assert — the "Security" category should have at least 3 items
        List<ProductionCheckItem> securityItems = checklist.stream()
                .filter(i -> i.getCategory().equals("Security"))
                .toList();
        assertThat(securityItems).hasSizeGreaterThanOrEqualTo(3);
    }
}
