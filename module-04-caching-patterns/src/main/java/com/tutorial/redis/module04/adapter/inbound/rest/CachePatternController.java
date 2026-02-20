package com.tutorial.redis.module04.adapter.inbound.rest;

import com.tutorial.redis.module04.application.dto.ExchangeRateResponse;
import com.tutorial.redis.module04.domain.model.ExchangeRate;
import com.tutorial.redis.module04.domain.model.ProductCatalog;
import com.tutorial.redis.module04.domain.model.TransactionEvent;
import com.tutorial.redis.module04.domain.port.inbound.BufferTransactionUseCase;
import com.tutorial.redis.module04.domain.port.inbound.GetExchangeRateUseCase;
import com.tutorial.redis.module04.domain.port.inbound.ManageProductCatalogUseCase;
import com.tutorial.redis.module04.domain.port.inbound.RefreshAheadCacheUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing endpoints for demonstrating various caching patterns:
 * Cache-Aside, Read-Through/Write-Through, Write-Behind, and Refresh-Ahead.
 */
@RestController
@RequestMapping("/api/v1/cache")
public class CachePatternController {

    private final GetExchangeRateUseCase getExchangeRateUseCase;
    private final ManageProductCatalogUseCase manageProductCatalogUseCase;
    private final BufferTransactionUseCase bufferTransactionUseCase;
    private final RefreshAheadCacheUseCase refreshAheadCacheUseCase;

    public CachePatternController(GetExchangeRateUseCase getExchangeRateUseCase,
                                  ManageProductCatalogUseCase manageProductCatalogUseCase,
                                  BufferTransactionUseCase bufferTransactionUseCase,
                                  RefreshAheadCacheUseCase refreshAheadCacheUseCase) {
        this.getExchangeRateUseCase = getExchangeRateUseCase;
        this.manageProductCatalogUseCase = manageProductCatalogUseCase;
        this.bufferTransactionUseCase = bufferTransactionUseCase;
        this.refreshAheadCacheUseCase = refreshAheadCacheUseCase;
    }

    // --- Cache-Aside Pattern: Exchange Rate ---

    @GetMapping("/exchange-rate/{pair}")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(@PathVariable String pair) {
        ExchangeRate rate = getExchangeRateUseCase.getRate(pair);
        ExchangeRateResponse response = new ExchangeRateResponse(
                rate.getCurrencyPair(),
                rate.getRate(),
                rate.getTimestamp(),
                "CACHE"
        );
        return ResponseEntity.ok(response);
    }

    // --- Read-Through / Write-Through Pattern: Product Catalog ---

    @GetMapping("/product/{productId}")
    public ResponseEntity<ProductCatalog> getProduct(@PathVariable String productId) {
        return manageProductCatalogUseCase.getProduct(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/product")
    public ResponseEntity<ProductCatalog> saveProduct(@RequestBody ProductCatalog product) {
        manageProductCatalogUseCase.saveProduct(product);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<Void> evictProduct(@PathVariable String productId) {
        manageProductCatalogUseCase.evictProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // --- Write-Behind Pattern: Transaction Buffer ---

    @PostMapping("/transaction/buffer")
    public ResponseEntity<Map<String, String>> bufferTransaction(@RequestBody TransactionEvent event) {
        bufferTransactionUseCase.bufferTransaction(event);
        return ResponseEntity.ok(Map.of(
                "status", "buffered",
                "transactionId", event.getTransactionId()
        ));
    }

    @PostMapping("/transaction/flush")
    public ResponseEntity<Map<String, Object>> flushTransactions(
            @RequestParam(defaultValue = "100") int batchSize) {
        int flushed = bufferTransactionUseCase.flushBuffer(batchSize);
        return ResponseEntity.ok(Map.of(
                "status", "flushed",
                "count", flushed
        ));
    }

    // --- Refresh-Ahead Pattern ---

    @GetMapping("/product/{productId}/refresh-ahead")
    public ResponseEntity<ProductCatalog> getProductRefreshAhead(@PathVariable String productId) {
        return refreshAheadCacheUseCase.getWithRefreshAhead(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
