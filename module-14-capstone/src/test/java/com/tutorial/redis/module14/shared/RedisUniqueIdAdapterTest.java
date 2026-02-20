package com.tutorial.redis.module14.shared;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module14.shared.adapter.outbound.redis.RedisUniqueIdAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisUniqueIdAdapter 整合測試")
class RedisUniqueIdAdapterTest extends AbstractRedisIntegrationTest {

    private RedisUniqueIdAdapter uniqueIdAdapter;

    @BeforeEach
    void setUpAdapter() {
        uniqueIdAdapter = new RedisUniqueIdAdapter(stringRedisTemplate);
    }

    @Test
    @DisplayName("nextSequence_IncrementsMonotonically — 序號應單調遞增")
    void nextSequence_IncrementsMonotonically() {
        // Act
        long seq1 = uniqueIdAdapter.nextSequence("test-counter");
        long seq2 = uniqueIdAdapter.nextSequence("test-counter");
        long seq3 = uniqueIdAdapter.nextSequence("test-counter");

        // Assert
        assertThat(seq1).isEqualTo(1);
        assertThat(seq2).isEqualTo(2);
        assertThat(seq3).isEqualTo(3);
        assertThat(seq3).isGreaterThan(seq2);
        assertThat(seq2).isGreaterThan(seq1);
    }

    @Test
    @DisplayName("nextSequence_DifferentKeys_IndependentCounters — 不同 Key 應有獨立計數器")
    void nextSequence_DifferentKeys_IndependentCounters() {
        // Act
        long seqA1 = uniqueIdAdapter.nextSequence("counter-a");
        long seqB1 = uniqueIdAdapter.nextSequence("counter-b");
        long seqA2 = uniqueIdAdapter.nextSequence("counter-a");

        // Assert
        assertThat(seqA1).isEqualTo(1);
        assertThat(seqB1).isEqualTo(1);
        assertThat(seqA2).isEqualTo(2);
    }
}
