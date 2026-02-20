package com.tutorial.redis.module08.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module08.domain.model.PersistenceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("RedisPersistenceInfoAdapter 整合測試")
class RedisPersistenceInfoAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisPersistenceInfoAdapter adapter;

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

    @Test
    @DisplayName("getServerInfo_ReturnsInfoSection — 查詢 server 區段資訊，應回傳非空的 Map")
    void getServerInfo_ReturnsInfoSection() {
        // Act
        Map<String, String> serverInfo = adapter.getServerInfo("server");

        // Assert
        assertThat(serverInfo).isNotEmpty();
        assertThat(serverInfo).containsKey("redis_version");
    }

    @Test
    @DisplayName("triggerBgsave_DoesNotThrow — 觸發 BGSAVE 不應拋出例外")
    void triggerBgsave_DoesNotThrow() {
        // Act & Assert
        assertThatCode(() -> adapter.triggerBgsave()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("triggerBgrewriteaof_DoesNotThrow — 觸發 BGREWRITEAOF 不應拋出例外")
    void triggerBgrewriteaof_DoesNotThrow() {
        // Act & Assert
        assertThatCode(() -> adapter.triggerBgrewriteaof()).doesNotThrowAnyException();
    }
}
