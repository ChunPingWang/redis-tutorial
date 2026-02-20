package com.tutorial.redis.module13.domain.model;

/**
 * Redis maxmemory eviction policies.
 *
 * <p>When Redis reaches its configured {@code maxmemory} limit, the eviction
 * policy determines which keys are removed to free memory for new writes.
 * Choosing the right policy depends on the workload (cache vs. persistent
 * data) and access patterns (LRU, LFU, TTL-based).</p>
 *
 * <ul>
 *   <li><b>noeviction</b> -- return errors when memory limit is reached</li>
 *   <li><b>allkeys-lru</b> -- evict least recently used keys from all keys</li>
 *   <li><b>allkeys-lfu</b> -- evict least frequently used keys from all keys</li>
 *   <li><b>allkeys-random</b> -- evict random keys from all keys</li>
 *   <li><b>volatile-lru</b> -- evict least recently used keys with TTL set</li>
 *   <li><b>volatile-lfu</b> -- evict least frequently used keys with TTL set</li>
 *   <li><b>volatile-random</b> -- evict random keys with TTL set</li>
 *   <li><b>volatile-ttl</b> -- evict keys with shortest TTL</li>
 * </ul>
 */
public enum EvictionPolicy {

    NOEVICTION("noeviction"),
    ALLKEYS_LRU("allkeys-lru"),
    ALLKEYS_LFU("allkeys-lfu"),
    ALLKEYS_RANDOM("allkeys-random"),
    VOLATILE_LRU("volatile-lru"),
    VOLATILE_LFU("volatile-lfu"),
    VOLATILE_RANDOM("volatile-random"),
    VOLATILE_TTL("volatile-ttl");

    private final String redisName;

    EvictionPolicy(String redisName) {
        this.redisName = redisName;
    }

    /**
     * Returns the policy name as used in {@code CONFIG SET maxmemory-policy}.
     */
    public String getRedisName() {
        return redisName;
    }

    /**
     * Resolves a Redis policy name (e.g. {@code "allkeys-lru"}) to the
     * corresponding enum constant.
     *
     * @param redisName the policy name returned by Redis INFO or CONFIG
     * @return the matching enum constant, or {@link #NOEVICTION} if unknown
     */
    public static EvictionPolicy fromRedisName(String redisName) {
        for (EvictionPolicy policy : values()) {
            if (policy.redisName.equalsIgnoreCase(redisName)) {
                return policy;
            }
        }
        return NOEVICTION;
    }
}
