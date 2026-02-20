package com.tutorial.redis.module10.domain.service;

import com.tutorial.redis.module10.domain.model.HashSlotInfo;
import com.tutorial.redis.module10.domain.model.HashTagAnalysis;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Domain service that performs CRC16-based hash slot calculations for Redis Cluster.
 *
 * <p>Redis Cluster partitions key space into 16384 hash slots. Each key is mapped
 * to a slot using {@code CRC16(key) mod 16384}. If the key contains a hash tag
 * (a substring enclosed in the first pair of curly braces, e.g. "{user}:cart"),
 * only the hash tag content is used for slot computation, allowing related keys
 * to be co-located on the same node.</p>
 *
 * <p>This is a pure domain service with no framework dependencies.</p>
 */
public class HashSlotCalculator {

    private static final int TOTAL_SLOTS = 16384;

    /**
     * CRC16 lookup table using the CCITT polynomial (0x1021).
     * This matches the Redis Cluster CRC16 implementation.
     */
    private static final int[] CRC16_TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i << 8;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
            }
            CRC16_TABLE[i] = crc & 0xFFFF;
        }
    }

    /**
     * Calculates the hash slot for the given key.
     *
     * <p>If the key contains a hash tag (content within the first pair of
     * braces), only the hash tag is used for CRC16 calculation. Otherwise,
     * the entire key is used.</p>
     *
     * @param key the Redis key
     * @return the hash slot number (0-16383)
     * @throws NullPointerException if key is null
     */
    public int calculateSlot(String key) {
        Objects.requireNonNull(key, "key must not be null");

        String hashTag = extractHashTag(key);
        String effectiveKey = hashTag != null ? hashTag : key;
        return crc16(effectiveKey.getBytes(StandardCharsets.UTF_8)) % TOTAL_SLOTS;
    }

    /**
     * Analyzes a single key and returns its hash slot information.
     *
     * @param key the Redis key to analyze
     * @return a {@link HashSlotInfo} containing the key, computed slot, and hash tag (if any)
     * @throws NullPointerException if key is null
     */
    public HashSlotInfo analyze(String key) {
        Objects.requireNonNull(key, "key must not be null");

        String hashTag = extractHashTag(key);
        int slot = calculateSlot(key);

        return new HashSlotInfo(key, slot, hashTag);
    }

    /**
     * Analyzes a list of keys to determine if they share the same hash tag and slot.
     *
     * @param keys the list of keys to analyze
     * @return a {@link HashTagAnalysis} describing whether all keys share the same slot
     * @throws NullPointerException     if keys is null
     * @throws IllegalArgumentException if keys is empty
     */
    public HashTagAnalysis analyzeHashTag(List<String> keys) {
        Objects.requireNonNull(keys, "keys must not be null");
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("keys must not be empty");
        }

        HashSlotInfo first = analyze(keys.getFirst());
        int commonSlot = first.getSlot();
        String commonHashTag = first.getHashTag();
        boolean sameSlot = true;

        for (int i = 1; i < keys.size(); i++) {
            HashSlotInfo info = analyze(keys.get(i));
            if (info.getSlot() != commonSlot) {
                sameSlot = false;
                commonSlot = -1;
                break;
            }
        }

        return new HashTagAnalysis(keys, commonHashTag, sameSlot, sameSlot ? first.getSlot() : -1);
    }

    /**
     * Extracts the hash tag from a key. The hash tag is the content between
     * the first occurrence of '{' and the next '}' after it. If no valid
     * hash tag is found (empty braces or no braces), returns null.
     *
     * @param key the key to extract the hash tag from
     * @return the hash tag content, or null if no valid hash tag exists
     */
    public String extractHashTag(String key) {
        int start = key.indexOf('{');
        if (start == -1) {
            return null;
        }
        int end = key.indexOf('}', start + 1);
        if (end == -1 || end == start + 1) {
            return null;
        }
        return key.substring(start + 1, end);
    }

    /**
     * Computes the CRC16 checksum (CCITT variant) used by Redis Cluster.
     *
     * @param data the byte array to compute the checksum for
     * @return the CRC16 checksum value
     */
    private int crc16(byte[] data) {
        int crc = 0;
        for (byte b : data) {
            crc = ((crc << 8) ^ CRC16_TABLE[((crc >> 8) ^ (b & 0xFF)) & 0xFF]) & 0xFFFF;
        }
        return crc;
    }
}
