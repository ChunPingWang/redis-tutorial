package com.tutorial.redis.module12.domain.model;

import java.util.Objects;

/**
 * Summarises the review data for a product stored within a RedisJSON document.
 *
 * <p>Nested inside {@link ProductDocument}, this value object holds the aggregate
 * average rating and total review count. The average rating can be updated
 * atomically via JSON.NUMINCRBY Lua scripts.</p>
 *
 * Mutable model with no-arg and all-args constructors.
 */
public class ReviewSummary {

    private double averageRating;
    private int count;

    public ReviewSummary() {
    }

    public ReviewSummary(double averageRating, int count) {
        this.averageRating = averageRating;
        this.count = count;
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReviewSummary that)) return false;
        return Double.compare(that.averageRating, averageRating) == 0
                && count == that.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(averageRating, count);
    }

    @Override
    public String toString() {
        return "ReviewSummary{averageRating=%s, count=%d}".formatted(averageRating, count);
    }
}
