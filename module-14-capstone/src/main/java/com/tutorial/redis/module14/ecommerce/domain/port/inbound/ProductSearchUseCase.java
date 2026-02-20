package com.tutorial.redis.module14.ecommerce.domain.port.inbound;

import com.tutorial.redis.module14.ecommerce.domain.model.Product;

import java.util.List;

/**
 * Inbound port for product search operations.
 *
 * <p>Defines use cases for indexing products into the search engine,
 * performing full-text searches, and providing autocomplete suggestions.</p>
 */
public interface ProductSearchUseCase {

    void indexProduct(Product product);

    List<String> searchProducts(String query);

    List<String> autocomplete(String prefix);
}
