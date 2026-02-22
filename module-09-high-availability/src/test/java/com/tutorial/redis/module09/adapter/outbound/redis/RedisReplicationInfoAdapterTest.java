package com.tutorial.redis.module09.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module09.domain.model.ReplicationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 主從複製資訊適配器整合測試
 * 驗證透過 Redis INFO 命令取得複製狀態（角色、連線 Replica 數量）及基本讀寫操作。
 * 屬於 Adapter 層，測試與 Redis 伺服器的實際互動，展示 Replication 資訊查詢機制。
 */
@DisplayName("RedisReplicationInfoAdapter 整合測試")
class RedisReplicationInfoAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisReplicationInfoAdapter adapter;

    // 驗證單機模式下取得的複製資訊角色為 master，且無已連線的 Replica
    @Test
    @DisplayName("getReplicationInfo_ReturnsMasterRole — 單機模式下查詢複製資訊，角色應為 master")
    void getReplicationInfo_ReturnsMasterRole() {
        // Act
        ReplicationInfo result = adapter.getReplicationInfo();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo("master");
        assertThat(result.getConnectedSlaves()).isEqualTo(0); // standalone has no replicas
    }

    // 驗證查詢 Redis INFO replication 區段時，應回傳包含 role 欄位的非空 Map
    @Test
    @DisplayName("getServerInfo_ReturnsInfoSection — 查詢 replication 區段，應回傳非空 Map")
    void getServerInfo_ReturnsInfoSection() {
        // Act
        Map<String, String> map = adapter.getServerInfo("replication");

        // Assert
        assertThat(map).isNotEmpty().containsKey("role");
    }

    // 驗證寫入資料後能正確讀取回相同的值（基本讀寫一致性）
    @Test
    @DisplayName("writeAndReadData_ReturnsCorrectValue — 寫入後讀取，應回傳正確的值")
    void writeAndReadData_ReturnsCorrectValue() {
        // Arrange
        adapter.writeData("ha-test:key1", "value1");

        // Act
        String result = adapter.readData("ha-test:key1");

        // Assert
        assertThat(result).isEqualTo("value1");
    }

    // 驗證讀取不存在的 Key 時應回傳 null（無資料情境）
    @Test
    @DisplayName("readData_WhenNotExists_ReturnsNull — 讀取不存在的 Key，應回傳 null")
    void readData_WhenNotExists_ReturnsNull() {
        // Act
        String result = adapter.readData("ha-test:nonexistent-key");

        // Assert
        assertThat(result).isNull();
    }
}
