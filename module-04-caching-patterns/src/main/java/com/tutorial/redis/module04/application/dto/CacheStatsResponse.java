package com.tutorial.redis.module04.application.dto;

/**
 * Response DTO for cache statistics.
 *
 * @param hits    the number of cache hits
 * @param misses  the number of cache misses
 * @param hitRate the cache hit rate (hits / (hits + misses))
 */
public record CacheStatsResponse(
        long hits,
        long misses,
        double hitRate
) {
}
