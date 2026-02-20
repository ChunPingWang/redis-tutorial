package com.tutorial.redis.module02.adapter.inbound.rest;

import com.tutorial.redis.module02.application.dto.StockLevelResponse;
import com.tutorial.redis.module02.domain.port.inbound.ManageStockUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for stock level management.
 *
 * <p>Demonstrates Redis String (atomic counter) operations through
 * inventory restock and purchase endpoints.</p>
 */
@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final ManageStockUseCase manageStockUseCase;

    public StockController(ManageStockUseCase manageStockUseCase) {
        this.manageStockUseCase = manageStockUseCase;
    }

    @PostMapping("/{productId}/restock")
    public ResponseEntity<StockLevelResponse> restockProduct(
            @PathVariable String productId,
            @RequestParam long quantity) {
        long newLevel = manageStockUseCase.restockProduct(productId, quantity);
        return ResponseEntity.ok(new StockLevelResponse(productId, newLevel));
    }

    @PostMapping("/{productId}/purchase")
    public ResponseEntity<StockLevelResponse> purchaseProduct(
            @PathVariable String productId,
            @RequestParam long quantity) {
        long newLevel = manageStockUseCase.purchaseProduct(productId, quantity);
        return ResponseEntity.ok(new StockLevelResponse(productId, newLevel));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockLevelResponse> getStockLevel(@PathVariable String productId) {
        return manageStockUseCase.getStockLevel(productId)
                .stream()
                .mapToObj(qty -> new StockLevelResponse(productId, qty))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
