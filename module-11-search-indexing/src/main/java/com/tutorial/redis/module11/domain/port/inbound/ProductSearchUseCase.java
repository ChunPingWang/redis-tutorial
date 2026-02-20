package com.tutorial.redis.module11.domain.port.inbound;

import com.tutorial.redis.module11.domain.model.ProductIndex;
import com.tutorial.redis.module11.domain.model.SearchResult;

import java.util.List;

/**
 * Inbound port for product search operations.
 *
 * <p>Provides use cases for indexing products and performing full-text,
 * category-based, and price-range searches via RediSearch.</p>
 */
public interface ProductSearchUseCase {

    /**
     * Indexes a batch of products by saving them as Redis Hashes
     * and ensuring the product search index exists.
     *
     * @param products the products to index
     */
    void indexProducts(List<ProductIndex> products);

    /**
     * Performs a full-text search across product name and description fields.
     *
     * @param query the search query string
     * @return search results containing matching products
     */
    SearchResult searchProducts(String query);

    /**
     * Searches for products filtered by category (TAG field).
     *
     * @param category the category to filter by
     * @return search results containing matching products
     */
    SearchResult searchByCategory(String category);

    /**
     * Searches for products within a given price range (NUMERIC field).
     *
     * @param minPrice minimum price (inclusive)
     * @param maxPrice maximum price (inclusive)
     * @return search results containing matching products
     */
    SearchResult searchByPriceRange(double minPrice, double maxPrice);
}
