package com.tutorial.redis.module12.adapter.inbound.rest;

import com.tutorial.redis.module12.domain.model.ProductDocument;
import com.tutorial.redis.module12.domain.model.VectorSearchResult;
import com.tutorial.redis.module12.domain.port.inbound.JsonDocumentUseCase;
import com.tutorial.redis.module12.domain.port.inbound.VectorSearchUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for RedisJSON document and Vector Similarity Search operations.
 *
 * <p>Exposes endpoints for managing product JSON documents (CRUD, partial updates)
 * and performing vector similarity searches over product embeddings.</p>
 *
 * <p>JSON document endpoints:</p>
 * <ul>
 *   <li>POST /api/json-vector/products — save a product document</li>
 *   <li>GET /api/json-vector/products/{productId} — retrieve a product</li>
 *   <li>DELETE /api/json-vector/products/{productId} — delete a product</li>
 *   <li>PATCH /api/json-vector/products/{productId}/price — update price</li>
 * </ul>
 *
 * <p>Vector search endpoints:</p>
 * <ul>
 *   <li>POST /api/json-vector/vector/search — KNN similarity search</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/json-vector")
public class JsonVectorController {

    private final JsonDocumentUseCase jsonDocumentUseCase;
    private final VectorSearchUseCase vectorSearchUseCase;

    public JsonVectorController(JsonDocumentUseCase jsonDocumentUseCase,
                                VectorSearchUseCase vectorSearchUseCase) {
        this.jsonDocumentUseCase = jsonDocumentUseCase;
        this.vectorSearchUseCase = vectorSearchUseCase;
    }

    /**
     * Saves a product document as a RedisJSON value.
     *
     * @param product the product document to persist
     * @return 200 OK on success
     */
    @PostMapping("/products")
    public ResponseEntity<Void> saveProduct(@RequestBody ProductDocument product) {
        jsonDocumentUseCase.saveProduct(product);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a product document by its identifier.
     *
     * @param productId the unique product identifier
     * @return the product document, or 404 if not found
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductDocument> getProduct(@PathVariable String productId) {
        ProductDocument product = jsonDocumentUseCase.getProduct(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    /**
     * Deletes a product document from Redis.
     *
     * @param productId the unique product identifier
     * @return 200 OK on success
     */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId) {
        jsonDocumentUseCase.deleteProduct(productId);
        return ResponseEntity.ok().build();
    }

    /**
     * Updates the price of an existing product document atomically.
     *
     * @param productId the unique product identifier
     * @param price     the new price value
     * @return 200 OK on success
     */
    @PatchMapping("/products/{productId}/price")
    public ResponseEntity<Void> updatePrice(@PathVariable String productId,
                                             @RequestParam double price) {
        jsonDocumentUseCase.updatePrice(productId, price);
        return ResponseEntity.ok().build();
    }

    /**
     * Performs a K-Nearest Neighbours similarity search using a query vector.
     *
     * @param queryVector the embedding to find similar products for
     * @param topK        the number of most similar products to return
     * @return list of vector search results ordered by similarity score
     */
    @PostMapping("/vector/search")
    public ResponseEntity<List<VectorSearchResult>> searchSimilar(
            @RequestBody float[] queryVector,
            @RequestParam int topK) {
        List<VectorSearchResult> results = vectorSearchUseCase.searchSimilarProducts(queryVector, topK);
        return ResponseEntity.ok(results);
    }
}
