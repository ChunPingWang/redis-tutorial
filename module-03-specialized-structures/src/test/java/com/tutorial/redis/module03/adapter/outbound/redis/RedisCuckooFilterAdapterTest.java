package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module03.domain.port.outbound.CuckooFilterPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisCuckooFilterAdapter 整合測試")
class RedisCuckooFilterAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private CuckooFilterPort cuckooFilterPort;

    private static final String FILTER_NAME = "test-cuckoo";

    @Test
    @DisplayName("createFilter_AndAdd_CanCheckExistence — 建立過濾器並新增後可查詢存在")
    void createFilter_AndAdd_CanCheckExistence() {
        cuckooFilterPort.createFilter(FILTER_NAME, 1000);

        cuckooFilterPort.add(FILTER_NAME, "item1");

        assertThat(cuckooFilterPort.mightContain(FILTER_NAME, "item1")).isTrue();
    }

    @Test
    @DisplayName("delete_WhenItemExists_RemovesItem — 刪除已存在的項目後查詢回傳 false")
    void delete_WhenItemExists_RemovesItem() {
        cuckooFilterPort.createFilter(FILTER_NAME, 1000);
        cuckooFilterPort.add(FILTER_NAME, "item-to-delete");

        boolean deleted = cuckooFilterPort.delete(FILTER_NAME, "item-to-delete");

        assertThat(deleted).isTrue();
        assertThat(cuckooFilterPort.mightContain(FILTER_NAME, "item-to-delete")).isFalse();
    }

    @Test
    @DisplayName("mightContain_WhenNotAdded_ReturnsFalse — 未新增的項目回傳 false")
    void mightContain_WhenNotAdded_ReturnsFalse() {
        cuckooFilterPort.createFilter(FILTER_NAME, 1000);

        assertThat(cuckooFilterPort.mightContain(FILTER_NAME, "non-existent-item")).isFalse();
    }

    @Test
    @DisplayName("add_MultipleItems_AllContained — 新增多個項目後全部可查詢")
    void add_MultipleItems_AllContained() {
        cuckooFilterPort.createFilter(FILTER_NAME, 1000);

        for (int i = 1; i <= 10; i++) {
            cuckooFilterPort.add(FILTER_NAME, "item-" + i);
        }

        for (int i = 1; i <= 10; i++) {
            assertThat(cuckooFilterPort.mightContain(FILTER_NAME, "item-" + i))
                    .as("item-%d should be contained", i)
                    .isTrue();
        }
    }
}
