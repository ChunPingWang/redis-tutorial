package com.tutorial.redis.module06.adapter.inbound.rest;

import com.tutorial.redis.module06.domain.model.AccountAggregate;
import com.tutorial.redis.module06.domain.model.ExchangeRateSnapshot;
import com.tutorial.redis.module06.domain.model.Order;
import com.tutorial.redis.module06.domain.port.inbound.ManageAccountUseCase;
import com.tutorial.redis.module06.domain.port.inbound.ManageOrderUseCase;
import com.tutorial.redis.module06.domain.port.inbound.QueryExchangeRateUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing endpoints for demonstrating Redis data modeling patterns:
 * <ul>
 *   <li>Account management using the Hash-per-entity pattern with Set secondary indexes</li>
 *   <li>Order management using the JSON String pattern with Set and Sorted Set indexes</li>
 *   <li>Exchange rate time-series using the Sorted Set pattern</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/data-modeling")
public class DataModelingController {

    private final ManageAccountUseCase manageAccountUseCase;
    private final ManageOrderUseCase manageOrderUseCase;
    private final QueryExchangeRateUseCase queryExchangeRateUseCase;

    public DataModelingController(ManageAccountUseCase manageAccountUseCase,
                                  ManageOrderUseCase manageOrderUseCase,
                                  QueryExchangeRateUseCase queryExchangeRateUseCase) {
        this.manageAccountUseCase = manageAccountUseCase;
        this.manageOrderUseCase = manageOrderUseCase;
        this.queryExchangeRateUseCase = queryExchangeRateUseCase;
    }

    // ===================== Account Endpoints =====================

    /**
     * Creates a new account stored as a Redis Hash with secondary indexes
     * on currency and status.
     */
    @PostMapping("/accounts")
    public ResponseEntity<Map<String, String>> createAccount(@RequestBody AccountAggregate account) {
        manageAccountUseCase.createAccount(account);
        return ResponseEntity.ok(Map.of(
                "status", "created",
                "accountId", account.getAccountId()
        ));
    }

    /**
     * Retrieves an account by its ID using {@code HGETALL} on the entity Hash.
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountAggregate> getAccount(@PathVariable String accountId) {
        return manageAccountUseCase.getAccount(accountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes an account and removes it from the currency and status secondary indexes.
     */
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<Map<String, String>> deleteAccount(@PathVariable String accountId) {
        manageAccountUseCase.deleteAccount(accountId);
        return ResponseEntity.ok(Map.of(
                "status", "deleted",
                "accountId", accountId
        ));
    }

    /**
     * Finds all accounts denominated in the given currency using the
     * Set-based secondary index {@code idx:account:currency:{currency}}.
     */
    @GetMapping("/accounts/currency/{currency}")
    public ResponseEntity<List<AccountAggregate>> findAccountsByCurrency(@PathVariable String currency) {
        List<AccountAggregate> accounts = manageAccountUseCase.findAccountsByCurrency(currency);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Finds all accounts with the given status using the
     * Set-based secondary index {@code idx:account:status:{status}}.
     */
    @GetMapping("/accounts/status/{status}")
    public ResponseEntity<List<AccountAggregate>> findAccountsByStatus(@PathVariable String status) {
        List<AccountAggregate> accounts = manageAccountUseCase.findAccountsByStatus(status);
        return ResponseEntity.ok(accounts);
    }

    // ===================== Order Endpoints =====================

    /**
     * Creates a new order stored as a JSON String with secondary indexes
     * on customerId (Set) and createdAt (Sorted Set).
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody Order order) {
        manageOrderUseCase.createOrder(order);
        return ResponseEntity.ok(Map.of(
                "status", "created",
                "orderId", order.getOrderId()
        ));
    }

    /**
     * Retrieves an order by its ID by deserializing the JSON String value.
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        return manageOrderUseCase.getOrder(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes an order and removes it from the customer and time secondary indexes.
     */
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable String orderId) {
        manageOrderUseCase.deleteOrder(orderId);
        return ResponseEntity.ok(Map.of(
                "status", "deleted",
                "orderId", orderId
        ));
    }

    /**
     * Finds all orders placed by a given customer using the
     * Set-based secondary index {@code idx:order:customer:{customerId}}.
     */
    @GetMapping("/orders/customer/{customerId}")
    public ResponseEntity<List<Order>> findOrdersByCustomer(@PathVariable String customerId) {
        List<Order> orders = manageOrderUseCase.findOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Finds orders created within the given epoch-millisecond range using the
     * Sorted Set time index {@code idx:order:created} via {@code ZRANGEBYSCORE}.
     */
    @GetMapping("/orders/time-range")
    public ResponseEntity<List<Order>> findOrdersByTimeRange(@RequestParam long from,
                                                             @RequestParam long to) {
        List<Order> orders = manageOrderUseCase.findOrdersByTimeRange(from, to);
        return ResponseEntity.ok(orders);
    }

    // ===================== Exchange Rate Endpoints =====================

    /**
     * Records a new exchange rate snapshot into the time series.
     * Stored as a Sorted Set entry with timestamp as the score.
     */
    @PostMapping("/rates")
    public ResponseEntity<Map<String, String>> recordRate(@RequestBody ExchangeRateSnapshot snapshot) {
        queryExchangeRateUseCase.recordRate(snapshot);
        return ResponseEntity.ok(Map.of(
                "status", "recorded",
                "currencyPair", snapshot.getCurrencyPair(),
                "timestamp", String.valueOf(snapshot.getTimestamp())
        ));
    }

    /**
     * Queries exchange rate snapshots for a currency pair within the given
     * epoch-millisecond range using {@code ZRANGEBYSCORE}.
     */
    @GetMapping("/rates/{currencyPair}")
    public ResponseEntity<List<ExchangeRateSnapshot>> queryRates(
            @PathVariable String currencyPair,
            @RequestParam long from,
            @RequestParam long to) {
        List<ExchangeRateSnapshot> snapshots = queryExchangeRateUseCase.queryRates(currencyPair, from, to);
        return ResponseEntity.ok(snapshots);
    }

    /**
     * Retrieves the latest (most recent) exchange rate for a currency pair
     * using {@code ZREVRANGEBYSCORE} with a limit of 1.
     */
    @GetMapping("/rates/{currencyPair}/latest")
    public ResponseEntity<ExchangeRateSnapshot> getLatestRate(@PathVariable String currencyPair) {
        return queryExchangeRateUseCase.getLatestRate(currencyPair)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
