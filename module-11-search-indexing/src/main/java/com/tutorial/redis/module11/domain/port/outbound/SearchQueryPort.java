package com.tutorial.redis.module11.domain.port.outbound;

import com.tutorial.redis.module11.domain.model.AggregationResult;
import com.tutorial.redis.module11.domain.model.SearchResult;

import java.util.List;

/**
 * Outbound port for RediSearch query operations (FT.SEARCH, FT.AGGREGATE).
 *
 * <p>Implemented by Redis adapter using Lua scripts (DefaultRedisScript) to invoke
 * RediSearch module commands.</p>
 */
public interface SearchQueryPort {

    /**
     * Executes a full-text search query against the given index.
     * Returns all matching documents (no pagination).
     *
     * @param indexName name of the RediSearch index
     * @param query     RediSearch query string (e.g. "wireless headphones")
     * @return search results containing matching documents
     */
    SearchResult search(String indexName, String query);

    /**
     * Executes a paginated full-text search query against the given index.
     *
     * @param indexName name of the RediSearch index
     * @param query     RediSearch query string
     * @param offset    zero-based offset for pagination
     * @param limit     maximum number of results to return
     * @return search results containing matching documents
     */
    SearchResult search(String indexName, String query, int offset, int limit);

    /**
     * Executes an aggregation query against the given index.
     *
     * @param indexName       name of the RediSearch index
     * @param query           RediSearch query string for filtering
     * @param aggregationArgs additional aggregation arguments (GROUPBY, REDUCE, etc.)
     * @return aggregation results containing grouped/reduced rows
     */
    AggregationResult aggregate(String indexName, String query, List<String> aggregationArgs);
}
