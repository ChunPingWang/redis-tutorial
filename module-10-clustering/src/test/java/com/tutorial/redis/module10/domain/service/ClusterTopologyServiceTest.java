package com.tutorial.redis.module10.domain.service;

import com.tutorial.redis.module10.domain.model.ClusterNodeInfo;
import com.tutorial.redis.module10.domain.model.ClusterTopology;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 ClusterTopologyService 的叢集拓撲生成邏輯。
 * 驗證推薦拓撲的節點數量、Master/Replica 比例，以及 16384 個 Hash Slot 的完整覆蓋與無間隙分配。
 * 屬於 Domain 層（領域服務），測試純業務邏輯，不依賴外部基礎設施。
 */
@DisplayName("ClusterTopologyService 領域服務測試")
class ClusterTopologyServiceTest {

    private final ClusterTopologyService service = new ClusterTopologyService();

    // 驗證預設推薦拓撲產生 6 個節點（3 Master + 3 Replica）且涵蓋全部 16384 個 Slot
    @Test
    @DisplayName("generateRecommendedTopology_ReturnsSixNodes — 推薦拓撲應有 6 個節點 (3 Master + 3 Replica)")
    void generateRecommendedTopology_ReturnsSixNodes() {
        // Act
        ClusterTopology topology = service.generateRecommendedTopology();

        // Assert
        assertThat(topology.getTotalNodes()).isEqualTo(6);
        assertThat(topology.getMasterCount()).isEqualTo(3);
        assertThat(topology.getReplicaCount()).isEqualTo(3);
        assertThat(topology.getTotalSlots()).isEqualTo(16384);
    }

    // 驗證 Master 節點的 Slot 範圍從 0 到 16383 完整覆蓋且無間隙
    @Test
    @DisplayName("generateRecommendedTopology_CoverAllSlots — 推薦拓撲的 Master 應涵蓋所有 16384 個 Slot")
    void generateRecommendedTopology_CoverAllSlots() {
        // Act
        ClusterTopology topology = service.generateRecommendedTopology();

        // Assert — verify slot ranges cover 0-16383 without gaps
        List<ClusterNodeInfo> masters = topology.getNodes().stream()
                .filter(node -> "master".equals(node.getRole()))
                .sorted((a, b) -> Integer.compare(a.getSlotStart(), b.getSlotStart()))
                .toList();

        assertThat(masters).hasSize(3);

        // First master should start at slot 0
        assertThat(masters.get(0).getSlotStart()).isEqualTo(0);

        // Last master should end at slot 16383
        assertThat(masters.get(masters.size() - 1).getSlotEnd()).isEqualTo(16383);

        // Verify no gaps between consecutive masters
        for (int i = 1; i < masters.size(); i++) {
            assertThat(masters.get(i).getSlotStart())
                    .isEqualTo(masters.get(i - 1).getSlotEnd() + 1);
        }
    }

    // 驗證自訂 Master 數量為 5 時，產生 10 個節點（5 Master + 5 Replica）
    @Test
    @DisplayName("generateTopology_CustomMasterCount_ReturnsCorrectNodes — 自訂 5 Master 拓撲應有 10 個節點")
    void generateTopology_CustomMasterCount_ReturnsCorrectNodes() {
        // Act
        ClusterTopology topology = service.generateTopology(5);

        // Assert
        assertThat(topology.getTotalNodes()).isEqualTo(10);
        assertThat(topology.getMasterCount()).isEqualTo(5);
        assertThat(topology.getReplicaCount()).isEqualTo(5);
    }
}
