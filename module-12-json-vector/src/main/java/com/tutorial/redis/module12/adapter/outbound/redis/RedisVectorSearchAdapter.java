package com.tutorial.redis.module12.adapter.outbound.redis;

import com.tutorial.redis.module12.domain.model.VectorIndexConfig;
import com.tutorial.redis.module12.domain.model.VectorSearchResult;
import com.tutorial.redis.module12.domain.port.outbound.VectorSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis adapter for Vector Similarity Search operations.
 *
 * <p>This adapter uses a simplified, educational approach to vector storage
 * and KNN search. Vectors are stored as comma-separated float strings inside
 * Redis Hash fields, and K-Nearest Neighbour searches are performed in Java
 * using cosine similarity.</p>
 *
 * <p>This design avoids the complexity of passing raw binary blobs through
 * Lua script ARGV parameters while still teaching the core concepts of
 * vector storage, similarity metrics, and nearest-neighbour retrieval.</p>
 *
 * <p>The {@link #createVectorIndex(VectorIndexConfig)} method uses a Lua script
 * to invoke FT.CREATE with a VECTOR schema field, demonstrating the actual
 * Redis command syntax even though the KNN search itself is performed
 * client-side for reliability.</p>
 */
@Component
public class RedisVectorSearchAdapter implements VectorSearchPort {

    private static final Logger log = LoggerFactory.getLogger(RedisVectorSearchAdapter.class);

    /**
     * Lua script for FT.CREATE with a VECTOR field.
     *
     * <p>KEYS[1] = index name<br>
     * ARGV[1] = prefix, ARGV[2] = vector field name, ARGV[3] = algorithm (FLAT/HNSW),
     * ARGV[4] = number of vector attributes (e.g. "6"),
     * ARGV[5] = dimensions, ARGV[6] = distance metric</p>
     *
     * <p>Example produced command:
     * {@code FT.CREATE idx ON HASH PREFIX 1 vec: SCHEMA embedding VECTOR FLAT 6
     *        TYPE FLOAT32 DIM 3 DISTANCE_METRIC COSINE}</p>
     */
    private static final DefaultRedisScript<String> FT_CREATE_VECTOR = new DefaultRedisScript<>(
            "return redis.call('FT.CREATE', KEYS[1], 'ON', 'HASH', 'PREFIX', '1', ARGV[1], " +
                    "'SCHEMA', ARGV[2], 'VECTOR', ARGV[3], ARGV[4], " +
                    "'TYPE', 'FLOAT32', 'DIM', ARGV[5], 'DISTANCE_METRIC', ARGV[6])",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisVectorSearchAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void createVectorIndex(VectorIndexConfig config) {
        // The number of attribute arguments after the algorithm name.
        // TYPE, FLOAT32, DIM, <dim>, DISTANCE_METRIC, <metric> = 6 args
        String numAttributes = "6";

        stringRedisTemplate.execute(FT_CREATE_VECTOR, List.of(config.getIndexName()),
                config.getPrefix(),
                config.getVectorField(),
                config.getAlgorithm(),
                numAttributes,
                String.valueOf(config.getDimensions()),
                config.getDistanceMetric());

        log.info("Created vector index '{}' with algorithm={}, dim={}, metric={}",
                config.getIndexName(), config.getAlgorithm(),
                config.getDimensions(), config.getDistanceMetric());
    }

    @Override
    public void storeVector(String key, String field, float[] vector) {
        String vectorString = floatArrayToString(vector);
        stringRedisTemplate.opsForHash().put(key, field, vectorString);
        log.debug("Stored vector at key='{}' field='{}' (dimensions={})", key, field, vector.length);
    }

    @Override
    public List<VectorSearchResult> knnSearch(String indexName, String vectorField,
                                               float[] queryVector, int k) {
        // Educational approach: read all vectors, compute cosine similarity in Java,
        // sort by similarity descending, and return the top-K results.

        // Derive the key prefix from the index name convention.
        // The index name follows the pattern "idx:<entity>-vec", and keys use "vec:<entity>:*".
        // For flexibility, scan all keys that have the vector field in their Hash.
        String prefix = derivePrefix(indexName);
        Set<String> keys = stringRedisTemplate.keys(prefix + "*");

        if (keys == null || keys.isEmpty()) {
            log.debug("No keys found with prefix '{}' for KNN search", prefix);
            return List.of();
        }

        List<VectorSearchResult> results = new ArrayList<>();

        for (String key : keys) {
            Object rawVector = stringRedisTemplate.opsForHash().get(key, vectorField);
            if (rawVector == null) {
                continue;
            }

            float[] storedVector = stringToFloatArray(rawVector.toString());
            if (storedVector.length != queryVector.length) {
                log.warn("Dimension mismatch for key='{}': expected={}, found={}",
                        key, queryVector.length, storedVector.length);
                continue;
            }

            double similarity = cosineSimilarity(queryVector, storedVector);

            // Collect all Hash fields as metadata
            Map<Object, Object> allFields = stringRedisTemplate.opsForHash().entries(key);
            Map<String, String> fieldMap = new HashMap<>();
            for (Map.Entry<Object, Object> entry : allFields.entrySet()) {
                String fieldKey = entry.getKey().toString();
                // Exclude the raw vector field from the result metadata for readability
                if (!fieldKey.equals(vectorField)) {
                    fieldMap.put(fieldKey, entry.getValue().toString());
                }
            }

            results.add(new VectorSearchResult(key, similarity, fieldMap));
        }

        // Sort by similarity descending (higher cosine similarity = more similar)
        results.sort(Comparator.comparingDouble(VectorSearchResult::getScore).reversed());

        // Return top-K results
        List<VectorSearchResult> topK = results.subList(0, Math.min(k, results.size()));
        log.info("KNN search on index='{}': scanned {} keys, returning top-{} results",
                indexName, keys.size(), topK.size());
        return new ArrayList<>(topK);
    }

    /**
     * Computes the cosine similarity between two vectors.
     *
     * <p>Cosine similarity measures the cosine of the angle between two vectors,
     * returning a value between -1 (opposite) and 1 (identical direction).
     * A value of 0 indicates orthogonality (no similarity).</p>
     *
     * <p>Formula: {@code cos(theta) = (A . B) / (||A|| * ||B||)}</p>
     *
     * @param a the first vector
     * @param b the second vector
     * @return the cosine similarity score
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }
        return dotProduct / denominator;
    }

    /**
     * Converts a float array to a comma-separated string for storage.
     *
     * @param vector the float array
     * @return comma-separated string representation (e.g. "0.1,0.2,0.3")
     */
    private String floatArrayToString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    /**
     * Parses a comma-separated string back into a float array.
     *
     * @param vectorString the comma-separated string (e.g. "0.1,0.2,0.3")
     * @return the parsed float array
     */
    private float[] stringToFloatArray(String vectorString) {
        String[] parts = vectorString.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    /**
     * Derives the key prefix from the vector index name.
     *
     * <p>Convention: index name "idx:products-vec" maps to key prefix "vec:product:".
     * This is a simplified heuristic for the educational module.</p>
     *
     * @param indexName the vector index name
     * @return the key prefix for scanning
     */
    private String derivePrefix(String indexName) {
        // Default prefix pattern: "vec:" + entity + ":"
        // For "idx:products-vec" -> "vec:product:"
        String name = indexName.replace("idx:", "");
        name = name.replace("-vec", "");
        // Remove trailing 's' for singular form (simple heuristic)
        if (name.endsWith("s")) {
            name = name.substring(0, name.length() - 1);
        }
        return "vec:" + name + ":";
    }
}
