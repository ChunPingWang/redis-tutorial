package com.tutorial.redis.module12.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.redis.module12.domain.model.ProductDocument;
import com.tutorial.redis.module12.domain.model.ProductVariant;
import com.tutorial.redis.module12.domain.port.inbound.JsonDocumentUseCase;
import com.tutorial.redis.module12.domain.port.outbound.JsonDocumentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service implementing RedisJSON product document use cases.
 *
 * <p>Coordinates between the REST layer and the {@link JsonDocumentPort} adapter,
 * handling JSON serialization/deserialization with Jackson {@link ObjectMapper}.
 * Each method maps to one or more RedisJSON commands executed through Lua scripts.</p>
 *
 * <p>Key design decisions:</p>
 * <ul>
 *   <li>Products are stored under the key pattern {@code product:{id}}</li>
 *   <li>The root JSONPath {@code $} is used for full document operations</li>
 *   <li>JSON.GET with path "$" returns an array (e.g. {@code [{...}]}),
 *       so the first element is extracted during deserialization</li>
 *   <li>Price updates use JSON.SET at {@code $.price} for atomic sub-path writes</li>
 *   <li>Variant additions use JSON.ARRAPPEND at {@code $.variants}</li>
 * </ul>
 */
@Service
public class JsonDocumentService implements JsonDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(JsonDocumentService.class);

    private static final String KEY_PREFIX = "product:";
    private static final String ROOT_PATH = "$";

    private final JsonDocumentPort jsonDocumentPort;
    private final ObjectMapper objectMapper;

    public JsonDocumentService(JsonDocumentPort jsonDocumentPort, ObjectMapper objectMapper) {
        this.jsonDocumentPort = jsonDocumentPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveProduct(ProductDocument product) {
        try {
            String json = objectMapper.writeValueAsString(product);
            String key = KEY_PREFIX + product.getProductId();
            jsonDocumentPort.setDocument(key, ROOT_PATH, json);
            log.info("Saved product document: key='{}'", key);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize ProductDocument to JSON", e);
        }
    }

    @Override
    public ProductDocument getProduct(String productId) {
        String key = KEY_PREFIX + productId;
        String json = jsonDocumentPort.getDocument(key, ROOT_PATH);

        if (json == null) {
            log.debug("Product not found: key='{}'", key);
            return null;
        }

        try {
            // JSON.GET with path "$" returns a JSON array like [{...}],
            // so we parse it as an array and extract the first element.
            ProductDocument[] documents = objectMapper.readValue(json, ProductDocument[].class);
            if (documents.length == 0) {
                log.warn("Empty array returned for key='{}'", key);
                return null;
            }
            log.debug("Retrieved product document: key='{}'", key);
            return documents[0];
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize ProductDocument from JSON: " + json, e);
        }
    }

    @Override
    public void deleteProduct(String productId) {
        String key = KEY_PREFIX + productId;
        jsonDocumentPort.deleteDocument(key);
        log.info("Deleted product document: key='{}'", key);
    }

    @Override
    public void updatePrice(String productId, double newPrice) {
        String key = KEY_PREFIX + productId;
        jsonDocumentPort.setDocument(key, "$.price", String.valueOf(newPrice));
        log.info("Updated price for product '{}' to {}", productId, newPrice);
    }

    @Override
    public void addVariant(String productId, ProductVariant variant) {
        try {
            String variantJson = objectMapper.writeValueAsString(variant);
            String key = KEY_PREFIX + productId;
            jsonDocumentPort.appendToArray(key, "$.variants", variantJson);
            log.info("Appended variant '{}' to product '{}'", variant.getSku(), productId);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize ProductVariant to JSON", e);
        }
    }
}
