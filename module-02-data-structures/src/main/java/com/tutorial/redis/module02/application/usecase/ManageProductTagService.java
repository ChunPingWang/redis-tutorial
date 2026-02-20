package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.port.inbound.ManageProductTagUseCase;
import com.tutorial.redis.module02.domain.port.outbound.ProductTagPort;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Application service implementing product tag management use cases.
 *
 * <p>Delegates to {@link ProductTagPort} for Redis Set operations.
 * Demonstrates SADD, SREM, SMEMBERS, SINTER (common tags),
 * and SUNION (all tags) for tag-based product categorization.</p>
 */
@Service
public class ManageProductTagService implements ManageProductTagUseCase {

    private final ProductTagPort productTagPort;

    public ManageProductTagService(ProductTagPort productTagPort) {
        this.productTagPort = productTagPort;
    }

    @Override
    public void tagProduct(String productId, Set<String> tags) {
        productTagPort.addTags(productId, tags);
    }

    @Override
    public void untagProduct(String productId, Set<String> tags) {
        productTagPort.removeTags(productId, tags);
    }

    @Override
    public Set<String> getProductTags(String productId) {
        return productTagPort.getTags(productId);
    }

    @Override
    public Set<String> findCommonTags(String productId1, String productId2) {
        return productTagPort.getCommonTags(productId1, productId2);
    }

    @Override
    public Set<String> findAllTags(String productId1, String productId2) {
        return productTagPort.getAllTags(productId1, productId2);
    }
}
