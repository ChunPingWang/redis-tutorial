package com.tutorial.redis.module02.adapter.inbound.rest;

import com.tutorial.redis.module02.application.dto.CartResponse;
import com.tutorial.redis.module02.domain.model.CartItem;
import com.tutorial.redis.module02.domain.port.inbound.ManageCartUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for shopping cart management.
 *
 * <p>Demonstrates Redis Hash operations through cart CRUD endpoints.
 * Each cart is a Redis Hash where field = productId and value = CartItem (JSON).</p>
 */
@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    private final ManageCartUseCase manageCartUseCase;

    public CartController(ManageCartUseCase manageCartUseCase) {
        this.manageCartUseCase = manageCartUseCase;
    }

    @PostMapping("/{customerId}/items")
    public ResponseEntity<Void> addToCart(
            @PathVariable String customerId,
            @RequestBody CartItem item) {
        manageCartUseCase.addToCart(customerId, item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{customerId}/items/{productId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable String customerId,
            @PathVariable String productId) {
        manageCartUseCase.removeFromCart(customerId, productId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{customerId}/items/{productId}")
    public ResponseEntity<Void> updateQuantity(
            @PathVariable String customerId,
            @PathVariable String productId,
            @RequestParam int quantity) {
        manageCartUseCase.updateQuantity(customerId, productId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable String customerId) {
        return manageCartUseCase.getCart(customerId)
                .map(CartResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> clearCart(@PathVariable String customerId) {
        manageCartUseCase.clearCart(customerId);
        return ResponseEntity.noContent().build();
    }
}
