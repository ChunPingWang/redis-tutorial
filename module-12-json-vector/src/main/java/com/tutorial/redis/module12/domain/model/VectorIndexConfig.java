package com.tutorial.redis.module12.domain.model;

import java.util.Objects;

/**
 * Configuration for creating a Redis Vector Similarity Search index.
 *
 * <p>Encapsulates the parameters needed for FT.CREATE with VECTOR fields,
 * including the algorithm type (FLAT or HNSW), vector dimensions, and
 * distance metric (COSINE, L2, IP).</p>
 *
 * Mutable model with no-arg and all-args constructors.
 */
public class VectorIndexConfig {

    private String indexName;
    private String prefix;
    private String vectorField;
    private String algorithm;
    private int dimensions;
    private String distanceMetric;

    public VectorIndexConfig() {
    }

    public VectorIndexConfig(String indexName, String prefix, String vectorField,
                             String algorithm, int dimensions, String distanceMetric) {
        this.indexName = indexName;
        this.prefix = prefix;
        this.vectorField = vectorField;
        this.algorithm = algorithm;
        this.dimensions = dimensions;
        this.distanceMetric = distanceMetric;
    }

    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getVectorField() { return vectorField; }
    public void setVectorField(String vectorField) { this.vectorField = vectorField; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public int getDimensions() { return dimensions; }
    public void setDimensions(int dimensions) { this.dimensions = dimensions; }

    public String getDistanceMetric() { return distanceMetric; }
    public void setDistanceMetric(String distanceMetric) { this.distanceMetric = distanceMetric; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VectorIndexConfig that)) return false;
        return Objects.equals(indexName, that.indexName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexName);
    }

    @Override
    public String toString() {
        return "VectorIndexConfig{indexName='%s', prefix='%s', vectorField='%s', algorithm='%s', dimensions=%d, distanceMetric='%s'}".formatted(
                indexName, prefix, vectorField, algorithm, dimensions, distanceMetric);
    }
}
