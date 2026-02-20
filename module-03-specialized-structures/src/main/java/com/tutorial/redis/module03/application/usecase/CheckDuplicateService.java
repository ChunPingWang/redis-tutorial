package com.tutorial.redis.module03.application.usecase;

import com.tutorial.redis.module03.domain.port.inbound.CheckDuplicateUseCase;
import com.tutorial.redis.module03.domain.port.outbound.BloomFilterPort;
import org.springframework.stereotype.Service;

/**
 * Application service implementing duplicate checking use cases.
 *
 * <p>Delegates to {@link BloomFilterPort} for Redis Bloom filter operations.
 * Demonstrates BF.RESERVE, BF.ADD, and BF.EXISTS for probabilistic set membership testing.</p>
 */
@Service
public class CheckDuplicateService implements CheckDuplicateUseCase {

    private final BloomFilterPort bloomFilterPort;

    public CheckDuplicateService(BloomFilterPort bloomFilterPort) {
        this.bloomFilterPort = bloomFilterPort;
    }

    @Override
    public void initializeFilter(String filterName, double errorRate, long capacity) {
        bloomFilterPort.createFilter(filterName, errorRate, capacity);
    }

    @Override
    public boolean markAsProcessed(String filterName, String itemId) {
        return bloomFilterPort.add(filterName, itemId);
    }

    @Override
    public boolean mightBeProcessed(String filterName, String itemId) {
        return bloomFilterPort.mightContain(filterName, itemId);
    }
}
