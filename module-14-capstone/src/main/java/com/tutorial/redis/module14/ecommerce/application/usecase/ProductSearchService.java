package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.model.Product;
import com.tutorial.redis.module14.ecommerce.domain.port.inbound.ProductSearchUseCase;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.ProductCachePort;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.ProductSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service implementing product search use cases.
 *
 * <p>Coordinates between the {@link ProductCachePort} for caching product
 * JSON and the {@link ProductSearchPort} for full-text search indexing
 * and autocomplete suggestions.</p>
 */
@Service
public class ProductSearchService implements ProductSearchUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final ProductSearchPort productSearchPort;
    private final ProductCachePort productCachePort;

    public ProductSearchService(ProductSearchPort productSearchPort,
                                ProductCachePort productCachePort) {
        this.productSearchPort = productSearchPort;
        this.productCachePort = productCachePort;
    }

    @Override
    public void indexProduct(Product product) {
        log.info("Indexing product {}", product.getProductId());

        // Cache product as simple string representation
        String productJson = product.getProductId() + "|" + product.getName() + "|"
                + product.getCategory() + "|" + product.getPrice() + "|"
                + product.getDescription() + "|" + product.getStockQuantity();
        productCachePort.cacheProduct(product.getProductId(), productJson);

        // Index product fields for search
        Map<String, String> fields = new HashMap<>();
        fields.put("name", product.getName());
        fields.put("category", product.getCategory());
        fields.put("price", String.valueOf(product.getPrice()));
        fields.put("description", product.getDescription());
        productSearchPort.indexProduct(product.getProductId(), fields);

        // Add product name as autocomplete suggestion
        productSearchPort.addSuggestion(product.getName(), 1.0);
    }

    @Override
    public List<String> searchProducts(String query) {
        log.info("Searching products with query: {}", query);
        return productSearchPort.search(query);
    }

    @Override
    public List<String> autocomplete(String prefix) {
        log.info("Autocomplete for prefix: {}", prefix);
        return productSearchPort.getSuggestions(prefix);
    }
}
