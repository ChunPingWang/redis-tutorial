package com.tutorial.redis.module11.domain.model;

import java.util.Objects;

/**
 * Represents an autocomplete suggestion returned by RediSearch FT.SUGGET.
 *
 * <p>The {@code score} indicates relevance — higher scores mean better matches.</p>
 *
 * Mutable model with no-arg and all-args constructors.
 */
public class SearchSuggestion {

    private String suggestion;
    private double score;

    public SearchSuggestion() {
    }

    public SearchSuggestion(String suggestion, double score) {
        this.suggestion = suggestion;
        this.score = score;
    }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchSuggestion that)) return false;
        return Double.compare(that.score, score) == 0
                && Objects.equals(suggestion, that.suggestion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suggestion, score);
    }

    @Override
    public String toString() {
        return "SearchSuggestion{suggestion='%s', score=%s}".formatted(suggestion, score);
    }
}
