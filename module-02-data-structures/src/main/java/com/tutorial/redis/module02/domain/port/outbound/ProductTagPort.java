package com.tutorial.redis.module02.domain.port.outbound;

import java.util.Set;

/**
 * Outbound port for product tag operations.
 * Uses Redis Set structure (unique, unordered collection of tags).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface ProductTagPort {

    void addTags(String productId, Set<String> tags);

    void removeTags(String productId, Set<String> tags);

    Set<String> getTags(String productId);

    boolean hasTag(String productId, String tag);

    Set<String> getCommonTags(String productId1, String productId2);

    Set<String> getAllTags(String productId1, String productId2);

    Set<String> getUniqueTags(String productId1, String productId2);
}
