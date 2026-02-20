package com.tutorial.redis.module05.application.usecase;

import com.tutorial.redis.module05.domain.port.inbound.BatchPriceQueryUseCase;
import com.tutorial.redis.module05.domain.port.outbound.PipelinePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Application service for batch price operations using Redis pipelines.
 * Delegates directly to the {@link PipelinePort} outbound port,
 * which batches multiple Redis commands into a single round-trip.
 */
@Service
public class BatchPriceQueryService implements BatchPriceQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(BatchPriceQueryService.class);

    private final PipelinePort pipelinePort;

    public BatchPriceQueryService(PipelinePort pipelinePort) {
        this.pipelinePort = pipelinePort;
    }

    @Override
    public Map<String, Double> batchGetPrices(List<String> productIds) {
        log.debug("Batch querying prices for {} products", productIds.size());
        return pipelinePort.batchGetPrices(productIds);
    }

    @Override
    public void batchSetPrices(Map<String, Double> prices) {
        log.debug("Batch setting prices for {} products", prices.size());
        pipelinePort.batchSetPrices(prices);
    }
}
