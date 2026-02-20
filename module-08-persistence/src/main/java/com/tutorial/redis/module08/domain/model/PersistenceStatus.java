package com.tutorial.redis.module08.domain.model;

import java.util.Objects;

/**
 * Current persistence status information retrieved from a running Redis instance.
 *
 * <p>Captures key metrics about both RDB and AOF persistence:
 * <ul>
 *   <li>{@code rdbEnabled} / {@code aofEnabled} — whether each strategy is active</li>
 *   <li>{@code rdbLastSaveTime} — epoch seconds of the most recent successful RDB save</li>
 *   <li>{@code aofCurrentSize} — current AOF file size in bytes</li>
 *   <li>{@code loading} — true when Redis is replaying persistence files at startup</li>
 *   <li>{@code lastBgsaveStatus} — "ok" or an error description from the last BGSAVE</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class PersistenceStatus {

    private boolean rdbEnabled;
    private boolean aofEnabled;
    private long rdbLastSaveTime;
    private long aofCurrentSize;
    private boolean loading;
    private String lastBgsaveStatus;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public PersistenceStatus() {
    }

    /**
     * Creates a PersistenceStatus with the specified values.
     *
     * @param rdbEnabled       whether RDB persistence is enabled
     * @param aofEnabled       whether AOF persistence is enabled
     * @param rdbLastSaveTime  epoch seconds of the last successful RDB save
     * @param aofCurrentSize   current AOF file size in bytes
     * @param loading          whether Redis is currently loading data from persistence files
     * @param lastBgsaveStatus the result of the last BGSAVE ("ok" or error message)
     */
    public PersistenceStatus(boolean rdbEnabled, boolean aofEnabled,
                             long rdbLastSaveTime, long aofCurrentSize,
                             boolean loading, String lastBgsaveStatus) {
        this.rdbEnabled = rdbEnabled;
        this.aofEnabled = aofEnabled;
        this.rdbLastSaveTime = rdbLastSaveTime;
        this.aofCurrentSize = aofCurrentSize;
        this.loading = loading;
        this.lastBgsaveStatus = lastBgsaveStatus;
    }

    public boolean isRdbEnabled() {
        return rdbEnabled;
    }

    public void setRdbEnabled(boolean rdbEnabled) {
        this.rdbEnabled = rdbEnabled;
    }

    public boolean isAofEnabled() {
        return aofEnabled;
    }

    public void setAofEnabled(boolean aofEnabled) {
        this.aofEnabled = aofEnabled;
    }

    public long getRdbLastSaveTime() {
        return rdbLastSaveTime;
    }

    public void setRdbLastSaveTime(long rdbLastSaveTime) {
        this.rdbLastSaveTime = rdbLastSaveTime;
    }

    public long getAofCurrentSize() {
        return aofCurrentSize;
    }

    public void setAofCurrentSize(long aofCurrentSize) {
        this.aofCurrentSize = aofCurrentSize;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public String getLastBgsaveStatus() {
        return lastBgsaveStatus;
    }

    public void setLastBgsaveStatus(String lastBgsaveStatus) {
        this.lastBgsaveStatus = lastBgsaveStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersistenceStatus that)) return false;
        return rdbEnabled == that.rdbEnabled
                && aofEnabled == that.aofEnabled
                && rdbLastSaveTime == that.rdbLastSaveTime
                && aofCurrentSize == that.aofCurrentSize
                && loading == that.loading
                && Objects.equals(lastBgsaveStatus, that.lastBgsaveStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rdbEnabled, aofEnabled, rdbLastSaveTime, aofCurrentSize,
                loading, lastBgsaveStatus);
    }

    @Override
    public String toString() {
        return "PersistenceStatus{rdbEnabled=%s, aofEnabled=%s, rdbLastSaveTime=%d, aofCurrentSize=%d, loading=%s, lastBgsaveStatus='%s'}".formatted(
                rdbEnabled, aofEnabled, rdbLastSaveTime, aofCurrentSize, loading, lastBgsaveStatus);
    }
}
