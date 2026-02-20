package com.tutorial.redis.module10.application.usecase;

import com.tutorial.redis.module10.domain.model.HashSlotInfo;
import com.tutorial.redis.module10.domain.model.HashTagAnalysis;
import com.tutorial.redis.module10.domain.port.inbound.HashSlotUseCase;
import com.tutorial.redis.module10.domain.service.HashSlotCalculator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing {@link HashSlotUseCase}.
 *
 * <p>Delegates hash slot calculation and hash tag analysis to the
 * {@link HashSlotCalculator} domain service. This thin application layer
 * exists to decouple the inbound adapter (REST controller) from the
 * domain service, following hexagonal architecture conventions.</p>
 */
@Service
public class HashSlotService implements HashSlotUseCase {

    private final HashSlotCalculator hashSlotCalculator;

    public HashSlotService(HashSlotCalculator hashSlotCalculator) {
        this.hashSlotCalculator = hashSlotCalculator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HashSlotInfo calculateSlot(String key) {
        return hashSlotCalculator.analyze(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HashTagAnalysis analyzeHashTag(List<String> keys) {
        return hashSlotCalculator.analyzeHashTag(keys);
    }
}
