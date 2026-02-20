package com.tutorial.redis.module02.domain.port.inbound;

import java.util.Set;

/**
 * Inbound port: manage product tags using Redis Set structure.
 */
public interface ManageProductTagUseCase {

    void tagProduct(String productId, Set<String> tags);

    void untagProduct(String productId, Set<String> tags);

    Set<String> getProductTags(String productId);

    Set<String> findCommonTags(String productId1, String productId2);

    Set<String> findAllTags(String productId1, String productId2);
}
