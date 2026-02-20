package com.tutorial.redis.module12.application.usecase;

import com.tutorial.redis.module12.domain.model.VectorIndexConfig;
import com.tutorial.redis.module12.domain.model.VectorSearchResult;
import com.tutorial.redis.module12.domain.port.inbound.VectorSearchUseCase;
import com.tutorial.redis.module12.domain.port.outbound.VectorSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing vector similarity search use cases.
 *
 * <p>Coordinates between the REST layer and the {@link VectorSearchPort} adapter
 * to manage product vector embeddings and perform similarity searches.</p>
 *
 * <p>Key conventions:</p>
 * <ul>
 *   <li>Product vectors are stored under {@code vec:product:{productId}}</li>
 *   <li>The vector field name within each Hash is {@code embedding}</li>
 *   <li>The default index name is {@code idx:products-vec}</li>
 * </ul>
 */
@Service
public class VectorSearchService implements VectorSearchUseCase {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private static final String VECTOR_KEY_PREFIX = "vec:product:";
    private static final String EMBEDDING_FIELD = "embedding";
    private static final String INDEX_NAME = "idx:products-vec";

    private final VectorSearchPort vectorSearchPort;

    public VectorSearchService(VectorSearchPort vectorSearchPort) {
        this.vectorSearchPort = vectorSearchPort;
    }

    @Override
    public void createProductVectorIndex(VectorIndexConfig config) {
        vectorSearchPort.createVectorIndex(config);
        log.info("Created product vector index: {}", config.getIndexName());
    }

    @Override
    public void storeProductVector(String productId, float[] embedding) {
        String key = VECTOR_KEY_PREFIX + productId;
        vectorSearchPort.storeVector(key, EMBEDDING_FIELD, embedding);
        log.info("Stored embedding for product '{}' (dimensions={})", productId, embedding.length);
    }

    @Override
    public List<VectorSearchResult> searchSimilarProducts(float[] queryVector, int topK) {
        List<VectorSearchResult> results = vectorSearchPort.knnSearch(
                INDEX_NAME, EMBEDDING_FIELD, queryVector, topK);
        log.info("Similarity search returned {} results (requested top-{})", results.size(), topK);
        return results;
    }
}
