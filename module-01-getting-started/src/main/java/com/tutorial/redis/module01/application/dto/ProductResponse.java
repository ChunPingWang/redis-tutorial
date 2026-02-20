package com.tutorial.redis.module01.application.dto;

import com.tutorial.redis.module01.domain.model.Product;
import java.math.BigDecimal;

public record ProductResponse(
        String productId,
        String name,
        BigDecimal price,
        String category,
        int stockQuantity
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getCategory(),
                product.getStockQuantity()
        );
    }
}
