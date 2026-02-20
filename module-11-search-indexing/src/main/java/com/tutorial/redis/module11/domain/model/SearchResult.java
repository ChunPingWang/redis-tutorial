package com.tutorial.redis.module11.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generic wrapper for RediSearch FT.SEARCH results.
 *
 * <p>Each document is represented as a {@code Map<String, String>} of field name to value,
 * matching the raw key-value pairs returned by FT.SEARCH.</p>
 *
 * Mutable model with no-arg and all-args constructors.
 */
public class SearchResult {

    private long totalResults;
    private List<Map<String, String>> documents;

    public SearchResult() {
        this.documents = new ArrayList<>();
    }

    public SearchResult(long totalResults, List<Map<String, String>> documents) {
        this.totalResults = totalResults;
        this.documents = documents != null ? documents : new ArrayList<>();
    }

    public long getTotalResults() { return totalResults; }
    public void setTotalResults(long totalResults) { this.totalResults = totalResults; }

    public List<Map<String, String>> getDocuments() { return documents; }
    public void setDocuments(List<Map<String, String>> documents) { this.documents = documents; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchResult that)) return false;
        return totalResults == that.totalResults && Objects.equals(documents, that.documents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalResults, documents);
    }

    @Override
    public String toString() {
        return "SearchResult{totalResults=%d, documentCount=%d}".formatted(
                totalResults, documents.size());
    }
}
