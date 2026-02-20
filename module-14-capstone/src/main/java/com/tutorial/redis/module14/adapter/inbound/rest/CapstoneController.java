package com.tutorial.redis.module14.adapter.inbound.rest;

import com.tutorial.redis.module14.ecommerce.domain.model.CartItem;
import com.tutorial.redis.module14.ecommerce.domain.port.inbound.CartUseCase;
import com.tutorial.redis.module14.ecommerce.domain.port.inbound.ProductSearchUseCase;
import com.tutorial.redis.module14.ecommerce.domain.port.inbound.RateLimitUseCase;
import com.tutorial.redis.module14.ecommerce.domain.port.inbound.StoreLocatorUseCase;
import com.tutorial.redis.module14.finance.domain.port.inbound.AccountManagementUseCase;
import com.tutorial.redis.module14.finance.domain.port.inbound.RiskAlertUseCase;
import com.tutorial.redis.module14.finance.domain.port.inbound.TransactionUseCase;
import com.tutorial.redis.module14.shared.application.usecase.DistributedLockService;
import com.tutorial.redis.module14.shared.application.usecase.IdempotencyService;
import com.tutorial.redis.module14.shared.application.usecase.UniqueIdService;
import com.tutorial.redis.module14.shared.domain.model.UniqueId;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Capstone REST controller exposing finance, e-commerce, and shared infrastructure endpoints.
 *
 * <p>Aggregates all Module 14 use cases into a single unified API surface,
 * demonstrating how hexagonal architecture ports can be composed behind
 * a thin REST layer.</p>
 */
@RestController
@RequestMapping("/api/capstone")
public class CapstoneController {

    private final AccountManagementUseCase accountUseCase;
    private final TransactionUseCase transactionUseCase;
    private final RiskAlertUseCase riskAlertUseCase;
    private final CartUseCase cartUseCase;
    private final ProductSearchUseCase productSearchUseCase;
    private final StoreLocatorUseCase storeLocatorUseCase;
    private final RateLimitUseCase rateLimitUseCase;
    private final DistributedLockService lockService;
    private final IdempotencyService idempotencyService;
    private final UniqueIdService uniqueIdService;

    public CapstoneController(AccountManagementUseCase accountUseCase,
                              TransactionUseCase transactionUseCase,
                              RiskAlertUseCase riskAlertUseCase,
                              CartUseCase cartUseCase,
                              ProductSearchUseCase productSearchUseCase,
                              StoreLocatorUseCase storeLocatorUseCase,
                              RateLimitUseCase rateLimitUseCase,
                              DistributedLockService lockService,
                              IdempotencyService idempotencyService,
                              UniqueIdService uniqueIdService) {
        this.accountUseCase = accountUseCase;
        this.transactionUseCase = transactionUseCase;
        this.riskAlertUseCase = riskAlertUseCase;
        this.cartUseCase = cartUseCase;
        this.productSearchUseCase = productSearchUseCase;
        this.storeLocatorUseCase = storeLocatorUseCase;
        this.rateLimitUseCase = rateLimitUseCase;
        this.lockService = lockService;
        this.idempotencyService = idempotencyService;
        this.uniqueIdService = uniqueIdService;
    }

    /**
     * Health check endpoint.
     *
     * @return status map indicating the module is running
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "module", "14-capstone");
    }

    /**
     * Caches an account balance.
     *
     * @param accountId the account identifier
     * @param balance   the balance to cache
     * @return confirmation map
     */
    @PostMapping("/finance/account/{accountId}/balance")
    public Map<String, Object> cacheAccountBalance(@PathVariable String accountId,
                                                   @RequestParam double balance) {
        accountUseCase.cacheAccountBalance(accountId, balance);
        return Map.of("accountId", accountId, "balance", balance, "status", "cached");
    }

    /**
     * Retrieves a cached account balance.
     *
     * @param accountId the account identifier
     * @return map containing the account ID and balance
     */
    @GetMapping("/finance/account/{accountId}/balance")
    public Map<String, Object> getAccountBalance(@PathVariable String accountId) {
        Double balance = accountUseCase.getAccountBalance(accountId);
        return Map.of("accountId", accountId, "balance", balance != null ? balance : "not found");
    }

    /**
     * Adds an item to a customer's shopping cart.
     *
     * @param customerId the customer identifier
     * @param item       the cart item to add
     * @return confirmation map
     */
    @PostMapping("/ecommerce/cart/{customerId}")
    public Map<String, Object> addToCart(@PathVariable String customerId,
                                        @RequestBody CartItem item) {
        cartUseCase.addToCart(customerId, item);
        return Map.of("customerId", customerId, "item", item.getProductId(), "status", "added");
    }

    /**
     * Retrieves a customer's shopping cart contents.
     *
     * @param customerId the customer identifier
     * @return list of cart items
     */
    @GetMapping("/ecommerce/cart/{customerId}")
    public List<CartItem> getCart(@PathVariable String customerId) {
        return cartUseCase.getCart(customerId);
    }

    /**
     * Searches products by query string.
     *
     * @param q the search query
     * @return list of matching product results
     */
    @GetMapping("/ecommerce/search")
    public List<String> searchProducts(@RequestParam String q) {
        return productSearchUseCase.searchProducts(q);
    }

    /**
     * Acquires a distributed lock on a resource.
     *
     * @param resource the resource to lock
     * @param owner    the lock owner identifier
     * @return map indicating whether the lock was acquired
     */
    @PostMapping("/shared/lock/{resource}")
    public Map<String, Object> acquireLock(@PathVariable String resource,
                                           @RequestParam String owner) {
        boolean acquired = lockService.acquireLock(resource, owner, 30);
        return Map.of("resource", resource, "owner", owner, "acquired", acquired);
    }

    /**
     * Releases a distributed lock on a resource.
     *
     * @param resource the resource to unlock
     * @param owner    the lock owner identifier
     * @return map indicating whether the lock was released
     */
    @DeleteMapping("/shared/lock/{resource}")
    public Map<String, Object> releaseLock(@PathVariable String resource,
                                           @RequestParam String owner) {
        boolean released = lockService.releaseLock(resource, owner);
        return Map.of("resource", resource, "owner", owner, "released", released);
    }

    /**
     * Generates a distributed unique ID with the given prefix.
     *
     * @param prefix the ID prefix
     * @return the generated unique ID string
     */
    @PostMapping("/shared/unique-id/{prefix}")
    public Map<String, String> generateUniqueId(@PathVariable String prefix) {
        UniqueId uniqueId = uniqueIdService.generateId(prefix);
        return Map.of("id", uniqueId.toId());
    }
}
