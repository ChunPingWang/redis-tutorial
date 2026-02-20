package com.tutorial.redis.module11.domain.port.outbound;

import java.util.Map;

/**
 * Outbound port for RediSearch index management (FT.CREATE, FT.DROPINDEX, FT._LIST).
 *
 * <p>Implemented by Redis adapter using Lua scripts (DefaultRedisScript) to invoke
 * RediSearch module commands, since connection.execute() does not work with
 * Lettuce / Spring Data Redis 4.x.</p>
 */
public interface SearchIndexPort {

    /**
     * Creates a RediSearch index on Hash keys matching the given prefix.
     *
     * @param indexName name of the index (e.g. "idx:product")
     * @param prefix    key prefix to index (e.g. "product:")
     * @param schema    field definitions — key is the field name,
     *                  value is the RediSearch type (TEXT, TAG, NUMERIC, GEO)
     */
    void createIndex(String indexName, String prefix, Map<String, String> schema);

    /**
     * Drops (deletes) an existing RediSearch index.
     *
     * @param indexName name of the index to drop
     */
    void dropIndex(String indexName);

    /**
     * Checks whether a RediSearch index already exists.
     *
     * @param indexName name of the index
     * @return {@code true} if the index exists, {@code false} otherwise
     */
    boolean indexExists(String indexName);
}
