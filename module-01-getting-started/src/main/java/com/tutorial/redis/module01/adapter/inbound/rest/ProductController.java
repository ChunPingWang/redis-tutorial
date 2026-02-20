package com.tutorial.redis.module01.adapter.inbound.rest;

import com.tutorial.redis.module01.application.dto.ProductResponse;
import com.tutorial.redis.module01.domain.model.Product;
import com.tutorial.redis.module01.domain.port.inbound.CacheProductUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CacheProductUseCase cacheProductUseCase;

    public ProductController(CacheProductUseCase cacheProductUseCase) {
        this.cacheProductUseCase = cacheProductUseCase;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String productId) {
        return cacheProductUseCase.getProduct(productId)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> evictProduct(@PathVariable String productId) {
        cacheProductUseCase.evictProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
