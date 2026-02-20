package com.tutorial.redis.module14.ecommerce.domain.model;

/**
 * Result of a rate limiting check.
 *
 * <p>Encapsulates whether the request was allowed, how many tokens remain
 * in the current window, and the retry delay in milliseconds if the
 * request was denied.</p>
 */
public class RateLimitResult {

    private boolean allowed;
    private int remainingTokens;
    private long retryAfterMs;

    public RateLimitResult() {
    }

    public RateLimitResult(boolean allowed, int remainingTokens, long retryAfterMs) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
        this.retryAfterMs = retryAfterMs;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public int getRemainingTokens() {
        return remainingTokens;
    }

    public void setRemainingTokens(int remainingTokens) {
        this.remainingTokens = remainingTokens;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }

    public void setRetryAfterMs(long retryAfterMs) {
        this.retryAfterMs = retryAfterMs;
    }

    @Override
    public String toString() {
        return "RateLimitResult{allowed=" + allowed + ", remainingTokens=" + remainingTokens
                + ", retryAfterMs=" + retryAfterMs + '}';
    }
}
