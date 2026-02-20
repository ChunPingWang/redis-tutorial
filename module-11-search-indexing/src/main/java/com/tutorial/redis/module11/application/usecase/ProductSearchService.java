package com.tutorial.redis.module11.application.usecase;

import com.tutorial.redis.module11.domain.model.ProductIndex;
import com.tutorial.redis.module11.domain.model.SearchResult;
import com.tutorial.redis.module11.domain.port.inbound.ProductSearchUseCase;
import com.tutorial.redis.module11.domain.port.outbound.ProductDataPort;
import com.tutorial.redis.module11.domain.port.outbound.SearchIndexPort;
import com.tutorial.redis.module11.domain.port.outbound.SearchQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service implementing product search use cases.
 *
 * <p>Coordinates between {@link ProductDataPort} (product storage),
 * {@link SearchIndexPort} (index creation), and {@link SearchQueryPort}
 * (search execution) to provide full-text, category, and price-range
 * search capabilities over product data.</p>
 *
 * <p>The product index schema uses:</p>
 * <ul>
 *   <li>{@code name} — TEXT with WEIGHT 5.0 (boosted relevance)</li>
 *   <li>{@code description} — TEXT (standard relevance)</li>
 *   <li>{@code category} — TAG (exact-match filtering)</li>
 *   <li>{@code price} — NUMERIC SORTABLE (range queries and sorting)</li>
 *   <li>{@code brand} — TAG (exact-match filtering)</li>
 * </ul>
 */
@Service
public class ProductSearchService implements ProductSearchUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private static final String INDEX_NAME = "idx:products";
    private static final String KEY_PREFIX = "product:";

    private final SearchIndexPort searchIndexPort;
    private final SearchQueryPort searchQueryPort;
    private final ProductDataPort productDataPort;

    public ProductSearchService(SearchIndexPort searchIndexPort,
                                SearchQueryPort searchQueryPort,
                                ProductDataPort productDataPort) {
        this.searchIndexPort = searchIndexPort;
        this.searchQueryPort = searchQueryPort;
        this.productDataPort = productDataPort;
    }

    @Override
    public void indexProducts(List<ProductIndex> products) {
        // Save each product as a Redis Hash
        productDataPort.saveProducts(products);
        log.info("Saved {} products as Redis Hashes", products.size());

        // Create the search index if it does not already exist
        if (!searchIndexPort.indexExists(INDEX_NAME)) {
            Map<String, String> schema = buildProductSchema();
            searchIndexPort.createIndex(INDEX_NAME, KEY_PREFIX, schema);
            log.info("Created product search index '{}'", INDEX_NAME);
        } else {
            log.debug("Product search index '{}' already exists, skipping creation", INDEX_NAME);
        }
    }

    @Override
    public SearchResult searchProducts(String query) {
        return searchQueryPort.search(INDEX_NAME, query);
    }

    @Override
    public SearchResult searchByCategory(String category) {
        String query = "@category:{" + escapeTag(category) + "}";
        return searchQueryPort.search(INDEX_NAME, query);
    }

    @Override
    public SearchResult searchByPriceRange(double minPrice, double maxPrice) {
        String query = "@price:[" + minPrice + " " + maxPrice + "]";
        return searchQueryPort.search(INDEX_NAME, query);
    }

    /**
     * Builds the RediSearch schema for the product index.
     *
     * <p>Uses a {@link LinkedHashMap} to maintain field order for
     * predictable index creation.</p>
     *
     * @return map of field name to RediSearch type definition
     */
    private Map<String, String> buildProductSchema() {
        Map<String, String> schema = new LinkedHashMap<>();
        schema.put("name", "TEXT WEIGHT 5.0");
        schema.put("description", "TEXT");
        schema.put("category", "TAG");
        schema.put("price", "NUMERIC SORTABLE");
        schema.put("brand", "TAG");
        return schema;
    }

    /**
     * Escapes special characters in TAG field values for RediSearch queries.
     *
     * @param tag the raw tag value
     * @return the escaped tag value safe for use in {@code @field:{value}} queries
     */
    private String escapeTag(String tag) {
        // RediSearch TAG queries require escaping of certain characters
        return tag.replace("-", "\\-")
                  .replace(".", "\\.")
                  .replace("'", "\\'");
    }
}
