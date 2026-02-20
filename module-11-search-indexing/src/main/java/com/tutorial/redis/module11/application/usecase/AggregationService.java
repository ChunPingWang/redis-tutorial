package com.tutorial.redis.module11.application.usecase;

import com.tutorial.redis.module11.domain.model.AggregationResult;
import com.tutorial.redis.module11.domain.port.inbound.AggregationUseCase;
import com.tutorial.redis.module11.domain.port.outbound.SearchQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing RediSearch aggregation use cases.
 *
 * <p>Delegates to {@link SearchQueryPort} for FT.AGGREGATE operations.
 * Demonstrates GROUPBY and REDUCE (AVG) aggregation pipelines for
 * computing average product prices per category.</p>
 */
@Service
public class AggregationService implements AggregationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AggregationService.class);

    private final SearchQueryPort searchQueryPort;

    public AggregationService(SearchQueryPort searchQueryPort) {
        this.searchQueryPort = searchQueryPort;
    }

    @Override
    public AggregationResult aggregateAveragePriceByCategory(String indexName) {
        List<String> aggregationArgs = List.of(
                "GROUPBY", "1", "@category",
                "REDUCE", "AVG", "1", "@price", "AS", "avg_price"
        );

        AggregationResult result = searchQueryPort.aggregate(indexName, "*", aggregationArgs);
        log.info("Aggregated average price by category on index '{}': {} rows",
                indexName, result.getRows().size());
        return result;
    }
}
