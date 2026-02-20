package com.tutorial.redis.module13.domain.model;

/**
 * Snapshot of Redis memory usage information.
 *
 * <p>Populated from the {@code INFO memory} command, which provides detailed
 * memory statistics including current usage, peak usage, configured limits,
 * and the active eviction policy.</p>
 *
 * <p>Key fields:</p>
 * <ul>
 *   <li>{@code usedMemory} -- bytes currently allocated by the Redis allocator</li>
 *   <li>{@code maxMemory} -- configured memory limit (0 means no limit)</li>
 *   <li>{@code evictionPolicy} -- the active maxmemory-policy</li>
 *   <li>{@code memoryUsagePercentage} -- usedMemory / maxMemory as a percentage</li>
 *   <li>{@code peakMemory} -- historical peak memory consumption</li>
 * </ul>
 */
public class MemoryInfo {

    private final long usedMemory;
    private final long maxMemory;
    private final EvictionPolicy evictionPolicy;
    private final double memoryUsagePercentage;
    private final long peakMemory;

    public MemoryInfo(long usedMemory, long maxMemory, EvictionPolicy evictionPolicy,
                      double memoryUsagePercentage, long peakMemory) {
        this.usedMemory = usedMemory;
        this.maxMemory = maxMemory;
        this.evictionPolicy = evictionPolicy;
        this.memoryUsagePercentage = memoryUsagePercentage;
        this.peakMemory = peakMemory;
    }

    public long getUsedMemory() {
        return usedMemory;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    public double getMemoryUsagePercentage() {
        return memoryUsagePercentage;
    }

    public long getPeakMemory() {
        return peakMemory;
    }

    @Override
    public String toString() {
        return "MemoryInfo{usedMemory=" + usedMemory + ", maxMemory=" + maxMemory
                + ", evictionPolicy=" + evictionPolicy
                + ", memoryUsagePercentage=" + String.format("%.2f", memoryUsagePercentage)
                + ", peakMemory=" + peakMemory + '}';
    }
}
