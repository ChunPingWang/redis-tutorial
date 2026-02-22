package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module03.domain.port.outbound.UniqueVisitorPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis HyperLogLog 配接器整合測試
 * 驗證 UniqueVisitorPort 透過 Redis HyperLogLog（PFADD/PFCOUNT/PFMERGE）命令的實作正確性
 * 涵蓋新增訪客、近似計數與多日合併計算，屬於 Adapter 層（外部輸出端）
 */
@DisplayName("RedisUniqueVisitorAdapter 整合測試")
class RedisUniqueVisitorAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private UniqueVisitorPort uniqueVisitorPort;

    private static final String PAGE_ID = "page-home";

    // 驗證 PFADD 新增全新訪客時回傳 true（內部結構有更新）
    @Test
    @DisplayName("addVisitor_NewVisitor_ReturnsTrue — 新訪客回傳 true")
    void addVisitor_NewVisitor_ReturnsTrue() {
        boolean result = uniqueVisitorPort.addVisitor(PAGE_ID, "2026-02-20", "visitor-001");

        assertThat(result).isTrue();
    }

    // 驗證 PFADD 重複新增同一訪客時回傳 false（內部結構未變更）
    @Test
    @DisplayName("addVisitor_SameVisitorTwice_ReturnsFalse — 重複訪客回傳 false")
    void addVisitor_SameVisitorTwice_ReturnsFalse() {
        uniqueVisitorPort.addVisitor(PAGE_ID, "2026-02-20", "visitor-001");

        boolean result = uniqueVisitorPort.addVisitor(PAGE_ID, "2026-02-20", "visitor-001");

        assertThat(result).isFalse();
    }

    // 驗證 PFCOUNT 對 100 位訪客的近似計數在合理誤差範圍內（90~110）
    @Test
    @DisplayName("countVisitors_WhenMultipleVisitors_ReturnsApproximateCount — 100 位訪客計數接近 100")
    void countVisitors_WhenMultipleVisitors_ReturnsApproximateCount() {
        for (int i = 1; i <= 100; i++) {
            uniqueVisitorPort.addVisitor(PAGE_ID, "2026-02-20", "visitor-" + i);
        }

        long count = uniqueVisitorPort.countVisitors(PAGE_ID, "2026-02-20");

        // HyperLogLog has ~0.81% standard error, for 100 items we allow a generous margin
        assertThat(count).isBetween(90L, 110L);
    }

    // 驗證 PFMERGE 合併兩天的 HyperLogLog 後，去重計數接近 90（含重疊訪客）
    @Test
    @DisplayName("countMergedVisitors_WhenMultiplePeriods_ReturnsMergedCount — 合併多日計數約 90")
    void countMergedVisitors_WhenMultiplePeriods_ReturnsMergedCount() {
        // Day 1: 50 unique visitors (visitor-001 to visitor-050)
        for (int i = 1; i <= 50; i++) {
            uniqueVisitorPort.addVisitor(PAGE_ID, "2026-02-20", "visitor-" + String.format("%03d", i));
        }
        // Day 2: 50 unique visitors (visitor-041 to visitor-090), 10 overlap with day 1
        for (int i = 41; i <= 90; i++) {
            uniqueVisitorPort.addVisitor(PAGE_ID, "2026-02-21", "visitor-" + String.format("%03d", i));
        }

        String destKey = "analytics:uv:" + PAGE_ID + ":merged";
        List<String> sourceKeys = List.of(
                "analytics:uv:" + PAGE_ID + ":2026-02-20",
                "analytics:uv:" + PAGE_ID + ":2026-02-21"
        );
        long mergedCount = uniqueVisitorPort.countMergedVisitors(destKey, sourceKeys);

        // 90 unique visitors total (50 + 50 - 10 overlap), allow HLL approximation margin
        assertThat(mergedCount).isBetween(80L, 100L);
    }

    // 驗證尚無訪客時 PFCOUNT 回傳 0
    @Test
    @DisplayName("countVisitors_WhenEmpty_ReturnsZero — 無訪客時計數為零")
    void countVisitors_WhenEmpty_ReturnsZero() {
        long count = uniqueVisitorPort.countVisitors(PAGE_ID, "2026-02-20");

        assertThat(count).isZero();
    }
}
