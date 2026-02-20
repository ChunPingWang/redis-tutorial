package com.tutorial.redis.module10.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents the result of analyzing a set of keys for hash tag grouping.
 *
 * <p>In Redis Cluster, keys that share the same hash tag (the substring
 * between the first {@code {}} in the key) are guaranteed to map to the
 * same hash slot. This model captures:
 * <ul>
 *   <li>{@code keys} — the list of keys that were analyzed</li>
 *   <li>{@code hashTag} — the common hash tag extracted from the keys,
 *       or null if no hash tag is present</li>
 *   <li>{@code sameSlot} — whether all keys map to the same hash slot</li>
 *   <li>{@code slot} — the common hash slot if all keys share the same slot,
 *       or -1 if they do not</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class HashTagAnalysis {

    private List<String> keys;
    private String hashTag;
    private boolean sameSlot;
    private int slot;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public HashTagAnalysis() {
    }

    /**
     * Creates a HashTagAnalysis with the specified values.
     *
     * @param keys     the list of keys that were analyzed
     * @param hashTag  the common hash tag, or null if none
     * @param sameSlot whether all keys map to the same hash slot
     * @param slot     the common hash slot, or -1 if keys are in different slots
     */
    public HashTagAnalysis(List<String> keys, String hashTag, boolean sameSlot, int slot) {
        this.keys = Objects.requireNonNull(keys, "keys must not be null");
        this.hashTag = hashTag;
        this.sameSlot = sameSlot;
        this.slot = slot;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public String getHashTag() {
        return hashTag;
    }

    public void setHashTag(String hashTag) {
        this.hashTag = hashTag;
    }

    public boolean isSameSlot() {
        return sameSlot;
    }

    public void setSameSlot(boolean sameSlot) {
        this.sameSlot = sameSlot;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HashTagAnalysis that)) return false;
        return sameSlot == that.sameSlot
                && slot == that.slot
                && Objects.equals(keys, that.keys)
                && Objects.equals(hashTag, that.hashTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keys, hashTag, sameSlot, slot);
    }

    @Override
    public String toString() {
        return "HashTagAnalysis{keys=%s, hashTag='%s', sameSlot=%s, slot=%d}".formatted(
                keys, hashTag, sameSlot, slot);
    }
}
