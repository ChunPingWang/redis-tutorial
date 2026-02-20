package com.tutorial.redis.module12.domain.port.outbound;

/**
 * Outbound port for RedisJSON document operations.
 *
 * <p>Implemented by a Redis adapter that invokes RedisJSON commands
 * (JSON.SET, JSON.GET, JSON.DEL, JSON.NUMINCRBY, JSON.ARRAPPEND)
 * through Lua scripts.</p>
 */
public interface JsonDocumentPort {

    /**
     * Sets a JSON value at the given path within a key.
     *
     * @param key       the Redis key
     * @param path      the JSONPath expression (e.g. "$" for root)
     * @param jsonValue the JSON string to store
     */
    void setDocument(String key, String path, String jsonValue);

    /**
     * Retrieves the JSON value at the given path within a key.
     *
     * @param key  the Redis key
     * @param path the JSONPath expression
     * @return the JSON string at the specified path, or {@code null} if the key does not exist
     */
    String getDocument(String key, String path);

    /**
     * Deletes an entire JSON document.
     *
     * @param key the Redis key to delete
     */
    void deleteDocument(String key);

    /**
     * Atomically increments a numeric value at the given path.
     *
     * @param key   the Redis key
     * @param path  the JSONPath to the numeric field
     * @param value the amount to increment (may be negative for decrement)
     */
    void incrementNumber(String key, String path, double value);

    /**
     * Appends a JSON element to an array at the given path.
     *
     * @param key         the Redis key
     * @param path        the JSONPath to the array field
     * @param jsonElement the JSON string of the element to append
     */
    void appendToArray(String key, String path, String jsonElement);
}
