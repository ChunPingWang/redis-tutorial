package com.tutorial.redis.module12.domain.port.inbound;

import com.tutorial.redis.module12.domain.model.VectorIndexConfig;
import com.tutorial.redis.module12.domain.model.VectorSearchResult;

import java.util.List;

/**
 * Inbound port for Vector Similarity Search use cases.
 *
 * <p>Provides high-level operations for creating product vector indexes,
 * storing product embeddings, and performing similarity searches using
 * Redis Vector Similarity Search (VSS).</p>
 */
public interface VectorSearchUseCase {

    /**
     * Creates a vector index for product embeddings.
     *
     * @param config the vector index configuration
     */
    void createProductVectorIndex(VectorIndexConfig config);

    /**
     * Stores a vector embedding for a product.
     *
     * @param productId the unique product identifier
     * @param embedding the float array representing the product's embedding
     */
    void storeProductVector(String productId, float[] embedding);

    /**
     * Searches for products similar to the given query vector.
     *
     * @param queryVector the embedding to find similar products for
     * @param topK        the number of most similar products to return
     * @return a list of search results ordered by similarity score
     */
    List<VectorSearchResult> searchSimilarProducts(float[] queryVector, int topK);
}
