package com.tutorial.redis.module05.adapter.inbound.rest;

import com.tutorial.redis.module05.application.dto.StockDeductionRequest;
import com.tutorial.redis.module05.application.dto.TransferRequest;
import com.tutorial.redis.module05.domain.model.StockDeductionResult;
import com.tutorial.redis.module05.domain.model.TransferResult;
import com.tutorial.redis.module05.domain.port.inbound.AtomicStockDeductionUseCase;
import com.tutorial.redis.module05.domain.port.inbound.BatchPriceQueryUseCase;
import com.tutorial.redis.module05.domain.port.inbound.OptimisticBalanceUpdateUseCase;
import com.tutorial.redis.module05.domain.port.inbound.TransferMoneyUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller exposing endpoints for demonstrating Redis pipeline and transaction patterns:
 * <ul>
 *   <li>Batch price queries and updates via pipelines</li>
 *   <li>Atomic money transfers via MULTI/EXEC transactions</li>
 *   <li>Atomic stock deduction via Lua scripts</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/pipeline-tx")
public class PipelineTransactionController {

    private final BatchPriceQueryUseCase batchPriceQueryUseCase;
    private final TransferMoneyUseCase transferMoneyUseCase;
    private final OptimisticBalanceUpdateUseCase optimisticBalanceUpdateUseCase;
    private final AtomicStockDeductionUseCase atomicStockDeductionUseCase;

    public PipelineTransactionController(BatchPriceQueryUseCase batchPriceQueryUseCase,
                                         TransferMoneyUseCase transferMoneyUseCase,
                                         OptimisticBalanceUpdateUseCase optimisticBalanceUpdateUseCase,
                                         AtomicStockDeductionUseCase atomicStockDeductionUseCase) {
        this.batchPriceQueryUseCase = batchPriceQueryUseCase;
        this.transferMoneyUseCase = transferMoneyUseCase;
        this.optimisticBalanceUpdateUseCase = optimisticBalanceUpdateUseCase;
        this.atomicStockDeductionUseCase = atomicStockDeductionUseCase;
    }

    // --- Pipeline: Batch Price Operations ---

    /**
     * Queries prices for multiple products using Redis pipelines.
     * Accepts a list of product IDs in the request body and returns
     * a map of product ID to price (null if not found).
     */
    @PostMapping("/batch-prices/query")
    public ResponseEntity<Map<String, Double>> batchGetPrices(@RequestBody List<String> productIds) {
        Map<String, Double> prices = batchPriceQueryUseCase.batchGetPrices(productIds);
        return ResponseEntity.ok(prices);
    }

    /**
     * Sets prices for multiple products using Redis pipelines.
     * Accepts a map of product ID to price in the request body.
     */
    @PostMapping("/batch-prices/set")
    public ResponseEntity<Map<String, String>> batchSetPrices(@RequestBody Map<String, Double> prices) {
        batchPriceQueryUseCase.batchSetPrices(prices);
        return ResponseEntity.ok(Map.of("status", "success", "count", String.valueOf(prices.size())));
    }

    // --- Transaction: Money Transfer ---

    /**
     * Transfers money between two accounts using Redis MULTI/EXEC transactions.
     * The transfer is atomic: both debit and credit execute together or not at all.
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResult> transfer(@RequestBody TransferRequest request) {
        TransferResult result = transferMoneyUseCase.transfer(
                request.fromAccountId(),
                request.toAccountId(),
                request.amount()
        );
        return ResponseEntity.ok(result);
    }

    // --- Lua Script: Atomic Stock Deduction ---

    /**
     * Deducts stock for a product atomically using a Redis Lua script.
     * Returns the deduction result indicating success, insufficient stock, or key not found.
     */
    @PostMapping("/stock/deduct")
    public ResponseEntity<Map<String, String>> deductStock(@RequestBody StockDeductionRequest request) {
        StockDeductionResult result = atomicStockDeductionUseCase.deductStock(
                request.productId(),
                request.quantity()
        );
        return ResponseEntity.ok(Map.of(
                "productId", request.productId(),
                "quantity", String.valueOf(request.quantity()),
                "result", result.name()
        ));
    }

    /**
     * Initializes stock for a product. Creates or resets the stock level.
     */
    @PostMapping("/stock/initialize/{productId}")
    public ResponseEntity<Map<String, String>> initializeStock(
            @PathVariable String productId,
            @RequestParam(defaultValue = "100") long quantity) {
        atomicStockDeductionUseCase.initializeStock(productId, quantity);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "quantity", String.valueOf(quantity),
                "status", "initialized"
        ));
    }

    /**
     * Retrieves the current stock level for a product.
     * Returns 404 if the stock key does not exist.
     */
    @GetMapping("/stock/{productId}")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable String productId) {
        Optional<Long> stock = atomicStockDeductionUseCase.getStock(productId);
        return stock
                .map(qty -> ResponseEntity.ok(Map.<String, Object>of(
                        "productId", productId,
                        "stock", qty
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
