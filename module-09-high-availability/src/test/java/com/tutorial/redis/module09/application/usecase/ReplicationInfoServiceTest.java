package com.tutorial.redis.module09.application.usecase;

import com.tutorial.redis.module09.domain.model.ReadWriteStrategy;
import com.tutorial.redis.module09.domain.model.ReplicationInfo;
import com.tutorial.redis.module09.domain.port.outbound.ReplicationInfoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("ReplicationInfoService 單元測試")
@ExtendWith(MockitoExtension.class)
class ReplicationInfoServiceTest {

    @Mock
    private ReplicationInfoPort port;

    @InjectMocks
    private ReplicationInfoService service;

    @Test
    @DisplayName("getReplicationInfo_DelegatesToPort — 取得複製資訊應委派給 ReplicationInfoPort")
    void getReplicationInfo_DelegatesToPort() {
        // Arrange
        ReplicationInfo expected = new ReplicationInfo("master", 2, 12345L, 1048576, true);
        when(port.getReplicationInfo()).thenReturn(expected);

        // Act
        ReplicationInfo result = service.getReplicationInfo();

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(port, times(1)).getReplicationInfo();
    }

    @Test
    @DisplayName("listReadWriteStrategies_ReturnsFourStrategies — 列出讀寫策略應回傳 4 種")
    void listReadWriteStrategies_ReturnsFourStrategies() {
        // Act
        List<ReadWriteStrategy> strategies = service.listReadWriteStrategies();

        // Assert
        assertThat(strategies).hasSize(4);
    }
}
