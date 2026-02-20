package com.tutorial.redis.module11.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper for RediSearch FT.AGGREGATE results.
 *
 * <p>Each row is represented as a {@code Map<String, String>} of field name to value,
 * matching the grouped/reduced output returned by FT.AGGREGATE.</p>
 *
 * Mutable model with no-arg and all-args constructors.
 */
public class AggregationResult {

    private List<Map<String, String>> rows;

    public AggregationResult() {
        this.rows = new ArrayList<>();
    }

    public AggregationResult(List<Map<String, String>> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }

    public List<Map<String, String>> getRows() { return rows; }
    public void setRows(List<Map<String, String>> rows) { this.rows = rows; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AggregationResult that)) return false;
        return Objects.equals(rows, that.rows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows);
    }

    @Override
    public String toString() {
        return "AggregationResult{rowCount=%d}".formatted(rows.size());
    }
}
