package com.tutorial.redis.module12.domain.port.outbound;

import com.tutorial.redis.module12.domain.model.VectorIndexConfig;
import com.tutorial.redis.module12.domain.model.VectorSearchResult;

import java.util.List;

/**
 * Outbound port for Redis Vector Similarity Search operations.
 *
 * <p>Implemented by a Redis adapter that manages vector indexes
 * (FT.CREATE with VECTOR fields) and performs KNN queries via
 * Lua scripts.</p>
 */
public interface VectorSearchPort {

    /**
     * Creates a vector search index with the given configuration.
     *
     * @param config the vector index configuration (algorithm, dimensions, distance metric)
     */
    void createVectorIndex(VectorIndexConfig config);

    /**
     * Stores a vector embedding in a Redis Hash field.
     *
     * @param key    the Redis key for the document
     * @param field  the Hash field name to store the vector in
     * @param vector the float array representing the embedding
     */
    void storeVector(String key, String field, float[] vector);

    /**
     * Performs a K-Nearest Neighbours search against a vector index.
     *
     * @param indexName   the name of the vector index to search
     * @param vectorField the field containing the stored vectors
     * @param queryVector the query embedding to find neighbours for
     * @param k           the number of nearest neighbours to return
     * @return a list of search results ordered by similarity score
     */
    List<VectorSearchResult> knnSearch(String indexName, String vectorField,
                                       float[] queryVector, int k);
}
