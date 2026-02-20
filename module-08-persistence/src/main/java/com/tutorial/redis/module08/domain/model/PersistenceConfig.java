package com.tutorial.redis.module08.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Redis persistence configuration.
 *
 * <p>Encapsulates the settings that control how Redis persists data to disk.
 * The {@code mode} field selects the persistence strategy:
 * <ul>
 *   <li>{@code "rdb"} — periodic point-in-time snapshots (dump.rdb)</li>
 *   <li>{@code "aof"} — append-only file logging every write operation</li>
 *   <li>{@code "hybrid"} — RDB preamble with AOF tail (Redis 4.0+)</li>
 *   <li>{@code "none"} — persistence disabled</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class PersistenceConfig {

    private String mode;
    private List<String> rdbSaveRules;
    private String aofFsyncPolicy;
    private boolean aofUseRdbPreamble;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public PersistenceConfig() {
        this.rdbSaveRules = new ArrayList<>();
    }

    /**
     * Creates a PersistenceConfig with the specified settings.
     *
     * @param mode              the persistence mode: "rdb", "aof", "hybrid", or "none"
     * @param rdbSaveRules      RDB save rules, e.g. ["900 1", "300 10", "60 10000"]
     * @param aofFsyncPolicy    AOF fsync policy: "always", "everysec", or "no"
     * @param aofUseRdbPreamble true to enable RDB preamble in the AOF file (hybrid mode)
     */
    public PersistenceConfig(String mode, List<String> rdbSaveRules,
                             String aofFsyncPolicy, boolean aofUseRdbPreamble) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.rdbSaveRules = rdbSaveRules != null ? new ArrayList<>(rdbSaveRules) : new ArrayList<>();
        this.aofFsyncPolicy = aofFsyncPolicy;
        this.aofUseRdbPreamble = aofUseRdbPreamble;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<String> getRdbSaveRules() {
        return rdbSaveRules;
    }

    public void setRdbSaveRules(List<String> rdbSaveRules) {
        this.rdbSaveRules = rdbSaveRules != null ? new ArrayList<>(rdbSaveRules) : new ArrayList<>();
    }

    public String getAofFsyncPolicy() {
        return aofFsyncPolicy;
    }

    public void setAofFsyncPolicy(String aofFsyncPolicy) {
        this.aofFsyncPolicy = aofFsyncPolicy;
    }

    public boolean isAofUseRdbPreamble() {
        return aofUseRdbPreamble;
    }

    public void setAofUseRdbPreamble(boolean aofUseRdbPreamble) {
        this.aofUseRdbPreamble = aofUseRdbPreamble;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersistenceConfig that)) return false;
        return aofUseRdbPreamble == that.aofUseRdbPreamble
                && Objects.equals(mode, that.mode)
                && Objects.equals(rdbSaveRules, that.rdbSaveRules)
                && Objects.equals(aofFsyncPolicy, that.aofFsyncPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, rdbSaveRules, aofFsyncPolicy, aofUseRdbPreamble);
    }

    @Override
    public String toString() {
        return "PersistenceConfig{mode='%s', rdbSaveRules=%s, aofFsyncPolicy='%s', aofUseRdbPreamble=%s}".formatted(
                mode, rdbSaveRules, aofFsyncPolicy, aofUseRdbPreamble);
    }
}
