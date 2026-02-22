package com.tutorial.redis.module04.application.usecase;

import com.tutorial.redis.module04.domain.model.TransactionEvent;
import com.tutorial.redis.module04.domain.port.outbound.TransactionBufferPort;
import com.tutorial.redis.module04.domain.port.outbound.TransactionPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 交易緩衝服務單元測試。
 * 驗證 Write-Behind（寫回）模式的應用層邏輯：交易事件先寫入緩衝區，再批次刷入持久層。
 * 使用 Mockito 模擬緩衝端口與持久化端口，確保業務邏輯正確委派。
 * 屬於 Application 層（應用服務 / Use Case）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BufferTransactionService 單元測試")
class BufferTransactionServiceTest {

    @Mock
    private TransactionBufferPort bufferPort;

    @Mock
    private TransactionPersistencePort persistencePort;

    @InjectMocks
    private BufferTransactionService service;

    private TransactionEvent createEvent(String id) {
        return new TransactionEvent(id, "ACC-001", 500.0, "DEPOSIT", Instant.now());
    }

    // 驗證緩衝交易操作正確委派至 TransactionBufferPort
    @Test
    @DisplayName("bufferTransaction_DelegatesToPort — 緩衝交易委派至緩衝端口")
    void bufferTransaction_DelegatesToPort() {
        TransactionEvent event = createEvent("TXN-001");

        service.bufferTransaction(event);

        verify(bufferPort).buffer(event);
    }

    // 驗證刷新緩衝區時，從緩衝端口排空事件並批次寫入持久層，回傳處理數量
    @Test
    @DisplayName("flushBuffer_DrainsAndPersists_ReturnsCount — 排空緩衝並持久化後回傳數量")
    void flushBuffer_DrainsAndPersists_ReturnsCount() {
        TransactionEvent event1 = createEvent("TXN-010");
        TransactionEvent event2 = createEvent("TXN-011");
        TransactionEvent event3 = createEvent("TXN-012");
        List<TransactionEvent> batch = List.of(event1, event2, event3);
        when(bufferPort.drainBatch(10)).thenReturn(batch);

        int flushed = service.flushBuffer(10);

        assertThat(flushed).isEqualTo(3);
        verify(bufferPort).drainBatch(10);
        verify(persistencePort).saveAll(batch);
    }

    // 驗證緩衝區為空時，回傳零且不觸發持久化操作
    @Test
    @DisplayName("flushBuffer_WhenEmpty_ReturnsZero — 空緩衝區排空回傳零且不呼叫持久化")
    void flushBuffer_WhenEmpty_ReturnsZero() {
        when(bufferPort.drainBatch(10)).thenReturn(Collections.emptyList());

        int flushed = service.flushBuffer(10);

        assertThat(flushed).isEqualTo(0);
        verify(bufferPort).drainBatch(10);
        verify(persistencePort, never()).saveAll(anyList());
    }
}
