package com.tutorial.redis.module14.ecommerce.domain.port.outbound;

import java.util.List;
import java.util.Map;

/**
 * Outbound port for product search and autocomplete operations.
 *
 * <p>Abstracts RediSearch module commands ({@code FT.CREATE}, {@code FT.SEARCH},
 * {@code FT.SUGADD}, {@code FT.SUGGET}) executed via Lua scripts for indexing
 * products, performing full-text search, and providing autocomplete
 * suggestions.</p>
 */
public interface ProductSearchPort {

    void indexProduct(String productId, Map<String, String> fields);

    List<String> search(String query);

    void addSuggestion(String term, double score);

    List<String> getSuggestions(String prefix);
}
