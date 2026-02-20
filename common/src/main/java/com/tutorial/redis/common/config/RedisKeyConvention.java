package com.tutorial.redis.common.config;

/**
 * Utility class providing conventions for Redis key naming.
 *
 * <p>Keys follow the pattern {@code service:entity:id} or {@code service:entity:id:field}
 * using colon as the separator, which is the standard Redis key naming convention.</p>
 */
public final class RedisKeyConvention {

    public static final String SEPARATOR = ":";

    private RedisKeyConvention() {
        // Utility class - prevent instantiation
    }

    /**
     * Builds a Redis key in the format {@code service:entity:id}.
     *
     * @param service the service or domain name
     * @param entity  the entity type
     * @param id      the unique identifier
     * @return the constructed Redis key
     */
    public static String buildKey(String service, String entity, String id) {
        return service + SEPARATOR + entity + SEPARATOR + id;
    }

    /**
     * Builds a Redis key in the format {@code service:entity:id:field}.
     *
     * @param service the service or domain name
     * @param entity  the entity type
     * @param id      the unique identifier
     * @param field   the field or sub-key name
     * @return the constructed Redis key
     */
    public static String buildKey(String service, String entity, String id, String field) {
        return service + SEPARATOR + entity + SEPARATOR + id + SEPARATOR + field;
    }

    /**
     * Builds a Redis index key in the format {@code idx:indexName}.
     *
     * @param indexName the name of the index
     * @return the constructed index key
     */
    public static String buildIndexKey(String indexName) {
        return "idx" + SEPARATOR + indexName;
    }
}
