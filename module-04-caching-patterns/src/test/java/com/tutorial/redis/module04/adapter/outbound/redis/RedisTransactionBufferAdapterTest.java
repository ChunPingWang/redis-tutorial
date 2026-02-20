package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module04.domain.model.TransactionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisTransactionBufferAdapter 整合測試")
class RedisTransactionBufferAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisTransactionBufferAdapter adapter;

    private TransactionEvent createEvent(String id, String type) {
        return new TransactionEvent(id, "ACC-001", 100.0, type, Instant.now());
    }

    @Test
    @DisplayName("buffer_AndDrain_ReturnsEvents — 緩衝後可排空取回事件")
    void buffer_AndDrain_ReturnsEvents() {
        TransactionEvent event1 = createEvent("TXN-001", "DEPOSIT");
        TransactionEvent event2 = createEvent("TXN-002", "WITHDRAWAL");

        adapter.buffer(event1);
        adapter.buffer(event2);

        List<TransactionEvent> drained = adapter.drainBatch(10);

        assertThat(drained).hasSize(2);
        assertThat(drained.get(0).getTransactionId()).isEqualTo("TXN-001");
        assertThat(drained.get(1).getTransactionId()).isEqualTo("TXN-002");
    }

    @Test
    @DisplayName("drainBatch_WhenEmpty_ReturnsEmptyList — 空緩衝區排空回傳空列表")
    void drainBatch_WhenEmpty_ReturnsEmptyList() {
        List<TransactionEvent> drained = adapter.drainBatch(10);

        assertThat(drained).isEmpty();
    }

    @Test
    @DisplayName("size_WhenMultipleBuffered_ReturnsCount — 多筆緩衝後回傳正確數量")
    void size_WhenMultipleBuffered_ReturnsCount() {
        adapter.buffer(createEvent("TXN-010", "DEPOSIT"));
        adapter.buffer(createEvent("TXN-011", "DEPOSIT"));
        adapter.buffer(createEvent("TXN-012", "WITHDRAWAL"));

        long size = adapter.size();

        assertThat(size).isEqualTo(3);
    }

    @Test
    @DisplayName("drainBatch_RespectsBatchSize — 排空數量不超過批次上限")
    void drainBatch_RespectsBatchSize() {
        adapter.buffer(createEvent("TXN-020", "DEPOSIT"));
        adapter.buffer(createEvent("TXN-021", "DEPOSIT"));
        adapter.buffer(createEvent("TXN-022", "WITHDRAWAL"));
        adapter.buffer(createEvent("TXN-023", "DEPOSIT"));
        adapter.buffer(createEvent("TXN-024", "WITHDRAWAL"));

        List<TransactionEvent> drained = adapter.drainBatch(3);

        assertThat(drained).hasSize(3);
        // FIFO order: first 3 events
        assertThat(drained.get(0).getTransactionId()).isEqualTo("TXN-020");
        assertThat(drained.get(1).getTransactionId()).isEqualTo("TXN-021");
        assertThat(drained.get(2).getTransactionId()).isEqualTo("TXN-022");
        // 2 events remain in buffer
        assertThat(adapter.size()).isEqualTo(2);
    }
}
