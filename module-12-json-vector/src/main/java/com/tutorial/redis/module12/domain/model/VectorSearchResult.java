package com.tutorial.redis.module12.domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single result from a Redis Vector Similarity Search (VSS) query.
 *
 * <p>Each result carries the document identifier, the similarity score produced
 * by the chosen distance metric, and a map of the document's indexed fields.</p>
 *
 * Mutable model with no-arg and all-args constructors.
 */
public class VectorSearchResult {

    private String documentId;
    private double score;
    private Map<String, String> fields;

    public VectorSearchResult() {
        this.fields = new HashMap<>();
    }

    public VectorSearchResult(String documentId, double score, Map<String, String> fields) {
        this.documentId = documentId;
        this.score = score;
        this.fields = fields != null ? fields : new HashMap<>();
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public Map<String, String> getFields() { return fields; }
    public void setFields(Map<String, String> fields) { this.fields = fields; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VectorSearchResult that)) return false;
        return Double.compare(that.score, score) == 0
                && Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, score);
    }

    @Override
    public String toString() {
        return "VectorSearchResult{documentId='%s', score=%s, fieldCount=%d}".formatted(
                documentId, score, fields.size());
    }
}
