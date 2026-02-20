package com.tutorial.redis.module11.adapter.inbound.rest;

import com.tutorial.redis.module11.domain.model.AggregationResult;
import com.tutorial.redis.module11.domain.model.ProductIndex;
import com.tutorial.redis.module11.domain.model.SearchResult;
import com.tutorial.redis.module11.domain.model.SearchSuggestion;
import com.tutorial.redis.module11.domain.port.inbound.AggregationUseCase;
import com.tutorial.redis.module11.domain.port.inbound.AutocompleteUseCase;
import com.tutorial.redis.module11.domain.port.inbound.ProductSearchUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for RediSearch operations.
 *
 * <p>Exposes endpoints for product indexing, full-text search,
 * category/price-range filtering, aggregation, and autocomplete.</p>
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ProductSearchUseCase productSearchUseCase;
    private final AggregationUseCase aggregationUseCase;
    private final AutocompleteUseCase autocompleteUseCase;

    public SearchController(ProductSearchUseCase productSearchUseCase,
                            AggregationUseCase aggregationUseCase,
                            AutocompleteUseCase autocompleteUseCase) {
        this.productSearchUseCase = productSearchUseCase;
        this.aggregationUseCase = aggregationUseCase;
        this.autocompleteUseCase = autocompleteUseCase;
    }

    /**
     * Indexes a batch of products — saves them as Redis Hashes and
     * creates the RediSearch index if it does not already exist.
     *
     * @param products the list of products to index
     * @return 200 OK on success
     */
    @PostMapping("/products/index")
    public ResponseEntity<Void> indexProducts(@RequestBody List<ProductIndex> products) {
        productSearchUseCase.indexProducts(products);
        return ResponseEntity.ok().build();
    }

    /**
     * Performs a full-text search across product fields.
     *
     * @param query the RediSearch query string
     * @return search results containing matching products
     */
    @GetMapping("/products")
    public ResponseEntity<SearchResult> searchProducts(@RequestParam String query) {
        SearchResult result = productSearchUseCase.searchProducts(query);
        return ResponseEntity.ok(result);
    }

    /**
     * Searches for products matching a specific category.
     *
     * @param category the category TAG value
     * @return search results containing matching products
     */
    @GetMapping("/products/category/{category}")
    public ResponseEntity<SearchResult> searchByCategory(@PathVariable String category) {
        SearchResult result = productSearchUseCase.searchByCategory(category);
        return ResponseEntity.ok(result);
    }

    /**
     * Searches for products within a price range.
     *
     * @param min minimum price (inclusive)
     * @param max maximum price (inclusive)
     * @return search results containing matching products
     */
    @GetMapping("/products/price-range")
    public ResponseEntity<SearchResult> searchByPriceRange(@RequestParam double min,
                                                           @RequestParam double max) {
        SearchResult result = productSearchUseCase.searchByPriceRange(min, max);
        return ResponseEntity.ok(result);
    }

    /**
     * Aggregates average product price grouped by category.
     *
     * @param indexName the RediSearch index name (defaults to "idx:products")
     * @return aggregation result with category and avg_price per row
     */
    @GetMapping("/products/aggregate/avg-price")
    public ResponseEntity<AggregationResult> aggregateAveragePriceByCategory(
            @RequestParam(defaultValue = "idx:products") String indexName) {
        AggregationResult result = aggregationUseCase.aggregateAveragePriceByCategory(indexName);
        return ResponseEntity.ok(result);
    }

    /**
     * Adds suggestions to an autocomplete dictionary.
     *
     * @param dictKey     the suggestion dictionary key
     * @param suggestions map of suggestion text to score
     * @return 200 OK on success
     */
    @PostMapping("/autocomplete/{dictKey}")
    public ResponseEntity<Void> addSuggestions(@PathVariable String dictKey,
                                               @RequestBody Map<String, Double> suggestions) {
        autocompleteUseCase.addSuggestions(dictKey, suggestions);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves autocomplete suggestions matching the given prefix.
     *
     * @param dictKey    the suggestion dictionary key
     * @param prefix     the prefix to match against
     * @param max        maximum number of suggestions (default 5)
     * @return list of matching suggestions ordered by score
     */
    @GetMapping("/autocomplete/{dictKey}")
    public ResponseEntity<List<SearchSuggestion>> autocomplete(
            @PathVariable String dictKey,
            @RequestParam String prefix,
            @RequestParam(defaultValue = "5") int max) {
        List<SearchSuggestion> suggestions = autocompleteUseCase.getSuggestions(dictKey, prefix, max);
        return ResponseEntity.ok(suggestions);
    }
}
