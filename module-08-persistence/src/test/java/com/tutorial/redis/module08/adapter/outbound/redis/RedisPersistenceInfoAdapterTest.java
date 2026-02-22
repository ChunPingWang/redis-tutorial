package com.tutorial.redis.module08.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module08.domain.model.PersistenceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 測試 RedisPersistenceInfoAdapter 的持久化資訊查詢與操作觸發功能。
 * 驗證取得 RDB/AOF 持久化狀態、伺服器資訊，以及觸發 BGSAVE 和 BGREWRITEAOF 命令。
 * 屬於 Adapter 層（外部介面卡），直接與 Redis 伺服器互動的整合測試。
 */
@DisplayName("RedisPersistenceInfoAdapter 整合測試")
class RedisPersistenceInfoAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisPersistenceInfoAdapter adapter;

    // 驗證從 Redis 取得持久化狀態，包含 RDB 最後儲存時間與 BGSAVE 狀態
    @Test
    @DisplayName("getPersistenceStatus_ReturnsPersistenceInfo — 查詢持久化狀態，應回傳有效的 PersistenceStatus")
    void getPersistenceStatus_ReturnsPersistenceInfo() {
        // Act
        PersistenceStatus status = adapter.getPersistenceStatus();

        // Assert
        assertThat(status).isNotNull();
        assertThat(status.getRdbLastSaveTime()).isGreaterThanOrEqualTo(0);
        assertThat(status.getLastBgsaveStatus()).isNotNull().isNotEmpty();
    }

    // 驗證查詢 Redis INFO 的 server 區段，應包含 redis_version 等關鍵資訊
    @Test
    @DisplayName("getServerInfo_ReturnsInfoSection — 查詢 server 區段資訊，應回傳非空的 Map")
    void getServerInfo_ReturnsInfoSection() {
        // Act
        Map<String, String> serverInfo = adapter.getServerInfo("server");

        // Assert
        assertThat(serverInfo).isNotEmpty();
        assertThat(serverInfo).containsKey("redis_version");
    }

    // 驗證觸發 BGSAVE（背景 RDB 快照）命令不會拋出例外
    @Test
    @DisplayName("triggerBgsave_DoesNotThrow — 觸發 BGSAVE 不應拋出例外")
    void triggerBgsave_DoesNotThrow() {
        // Act & Assert
        assertThatCode(() -> adapter.triggerBgsave()).doesNotThrowAnyException();
    }

    // 驗證觸發 BGREWRITEAOF（背景 AOF 重寫）命令不會拋出例外
    @Test
    @DisplayName("triggerBgrewriteaof_DoesNotThrow — 觸發 BGREWRITEAOF 不應拋出例外")
    void triggerBgrewriteaof_DoesNotThrow() {
        // Act & Assert
        assertThatCode(() -> adapter.triggerBgrewriteaof()).doesNotThrowAnyException();
    }
}
