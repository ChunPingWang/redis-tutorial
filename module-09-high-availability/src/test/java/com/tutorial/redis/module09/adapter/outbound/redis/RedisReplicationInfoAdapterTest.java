package com.tutorial.redis.module09.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module09.domain.model.ReplicationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisReplicationInfoAdapter 整合測試")
class RedisReplicationInfoAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisReplicationInfoAdapter adapter;

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

    @Test
    @DisplayName("getServerInfo_ReturnsInfoSection — 查詢 replication 區段，應回傳非空 Map")
    void getServerInfo_ReturnsInfoSection() {
        // Act
        Map<String, String> map = adapter.getServerInfo("replication");

        // Assert
        assertThat(map).isNotEmpty().containsKey("role");
    }

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

    @Test
    @DisplayName("readData_WhenNotExists_ReturnsNull — 讀取不存在的 Key，應回傳 null")
    void readData_WhenNotExists_ReturnsNull() {
        // Act
        String result = adapter.readData("ha-test:nonexistent-key");

        // Assert
        assertThat(result).isNull();
    }
}
