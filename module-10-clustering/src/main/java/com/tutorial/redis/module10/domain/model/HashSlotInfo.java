package com.tutorial.redis.module10.domain.model;

import java.util.Objects;

/**
 * Represents the result of a CRC16 hash slot calculation for a given key.
 *
 * <p>Redis Cluster uses a 16384-slot hash space where each key is mapped to
 * a slot using {@code CRC16(key) mod 16384}. This model captures:
 * <ul>
 *   <li>{@code key} — the original key whose slot was calculated</li>
 *   <li>{@code slot} — the computed hash slot (0–16383)</li>
 *   <li>{@code hashTag} — the extracted hash tag if present (e.g. "{user}"
 *       from "{user}:cart"), or null if no hash tag exists</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class HashSlotInfo {

    private String key;
    private int slot;
    private String hashTag;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public HashSlotInfo() {
    }

    /**
     * Creates a HashSlotInfo with the specified values.
     *
     * @param key     the original key
     * @param slot    the computed hash slot (CRC16(key) mod 16384)
     * @param hashTag the extracted hash tag, or null if none
     */
    public HashSlotInfo(String key, int slot, String hashTag) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.slot = slot;
        this.hashTag = hashTag;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getHashTag() {
        return hashTag;
    }

    public void setHashTag(String hashTag) {
        this.hashTag = hashTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HashSlotInfo that)) return false;
        return slot == that.slot
                && Objects.equals(key, that.key)
                && Objects.equals(hashTag, that.hashTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, slot, hashTag);
    }

    @Override
    public String toString() {
        return "HashSlotInfo{key='%s', slot=%d, hashTag='%s'}".formatted(key, slot, hashTag);
    }
}
