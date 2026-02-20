package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module02.domain.port.outbound.ProductTagPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisProductTagAdapter 整合測試")
class RedisProductTagAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private ProductTagPort productTagPort;

    @Test
    @DisplayName("addTags_WhenNewProduct_CreatesTags — 為新商品建立標籤")
    void addTags_WhenNewProduct_CreatesTags() {
        productTagPort.addTags("PROD-001", Set.of("java", "spring", "redis"));

        Set<String> tags = productTagPort.getTags("PROD-001");
        assertThat(tags).containsExactlyInAnyOrder("java", "spring", "redis");
    }

    @Test
    @DisplayName("removeTags_WhenTagExists_RemovesTag — 移除商品標籤")
    void removeTags_WhenTagExists_RemovesTag() {
        productTagPort.addTags("PROD-002", Set.of("java", "spring", "redis"));

        productTagPort.removeTags("PROD-002", Set.of("java"));

        Set<String> tags = productTagPort.getTags("PROD-002");
        assertThat(tags).containsExactlyInAnyOrder("spring", "redis");
        assertThat(tags).doesNotContain("java");
    }

    @Test
    @DisplayName("getTags_WhenMultipleTags_ReturnsAll — 取得商品所有標籤")
    void getTags_WhenMultipleTags_ReturnsAll() {
        productTagPort.addTags("PROD-003", Set.of("backend", "database", "cache", "nosql"));

        Set<String> tags = productTagPort.getTags("PROD-003");

        assertThat(tags).hasSize(4);
        assertThat(tags).containsExactlyInAnyOrder("backend", "database", "cache", "nosql");
    }

    @Test
    @DisplayName("hasTag_WhenTagExists_ReturnsTrue — 檢查標籤是否存在")
    void hasTag_WhenTagExists_ReturnsTrue() {
        productTagPort.addTags("PROD-004", Set.of("java", "spring"));

        assertThat(productTagPort.hasTag("PROD-004", "java")).isTrue();
        assertThat(productTagPort.hasTag("PROD-004", "python")).isFalse();
    }

    @Test
    @DisplayName("getCommonTags_WhenOverlap_ReturnsIntersection — SINTER 取得共同標籤")
    void getCommonTags_WhenOverlap_ReturnsIntersection() {
        productTagPort.addTags("PROD-005", Set.of("java", "spring", "redis"));
        productTagPort.addTags("PROD-006", Set.of("python", "spring", "redis"));

        Set<String> common = productTagPort.getCommonTags("PROD-005", "PROD-006");

        assertThat(common).containsExactlyInAnyOrder("spring", "redis");
    }

    @Test
    @DisplayName("getAllTags_ReturnsMergedSet — SUNION 取得所有標籤聯集")
    void getAllTags_ReturnsMergedSet() {
        productTagPort.addTags("PROD-007", Set.of("java", "spring"));
        productTagPort.addTags("PROD-008", Set.of("python", "django"));

        Set<String> all = productTagPort.getAllTags("PROD-007", "PROD-008");

        assertThat(all).containsExactlyInAnyOrder("java", "spring", "python", "django");
    }

    @Test
    @DisplayName("getUniqueTags_ReturnsDifference — SDIFF 取得差集標籤")
    void getUniqueTags_ReturnsDifference() {
        productTagPort.addTags("PROD-009", Set.of("java", "spring", "redis"));
        productTagPort.addTags("PROD-010", Set.of("python", "spring", "redis"));

        Set<String> unique = productTagPort.getUniqueTags("PROD-009", "PROD-010");

        assertThat(unique).containsExactlyInAnyOrder("java");
    }

    @Test
    @DisplayName("key_FollowsNamingConvention — 驗證 key 符合 ecommerce:tags:* 命名規範")
    void key_FollowsNamingConvention() {
        productTagPort.addTags("PROD-KEY-001", Set.of("test"));

        Set<String> keys = stringRedisTemplate.keys("ecommerce:tags:PROD-KEY-001");
        assertThat(keys).isNotNull().hasSize(1);
        assertThat(keys.iterator().next()).isEqualTo("ecommerce:tags:PROD-KEY-001");
    }
}
