package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.module05.domain.model.StockDeductionResult;
import com.tutorial.redis.module05.domain.port.outbound.AtomicStockDeductionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Redis adapter implementing atomic stock deduction using a Lua script.
 * The Lua script executes atomically on the Redis server, providing a
 * race-condition-free check-and-deduct mechanism to prevent overselling.
 *
 * <p>Key format: {@code stock:{productId}}</p>
 *
 * <p>Lua script return values:
 * <ul>
 *   <li>{@code 1} — deduction successful</li>
 *   <li>{@code 0} — insufficient stock</li>
 *   <li>{@code -1} — key not found</li>
 * </ul>
 */
@Component
public class RedisAtomicStockDeductionAdapter implements AtomicStockDeductionPort {

    private static final Logger log = LoggerFactory.getLogger(RedisAtomicStockDeductionAdapter.class);
    private static final String KEY_PREFIX = "stock:";

    /**
     * Lua script for atomic stock deduction.
     * Checks if the stock key exists and has sufficient quantity before deducting.
     * Returns 1 on success, 0 for insufficient stock, -1 if key not found.
     */
    private static final DefaultRedisScript<Long> DEDUCT_STOCK_SCRIPT;

    static {
        DEDUCT_STOCK_SCRIPT = new DefaultRedisScript<>();
        DEDUCT_STOCK_SCRIPT.setScriptText("""
                local stock = tonumber(redis.call('GET', KEYS[1]))
                if stock == nil then return -1 end
                if stock < tonumber(ARGV[1]) then return 0 end
                redis.call('DECRBY', KEYS[1], ARGV[1])
                return 1
                """);
        DEDUCT_STOCK_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    public RedisAtomicStockDeductionAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically checks and deducts stock for a product using a server-side Lua script.
     * The entire check-and-deduct operation is atomic, preventing race conditions.
     */
    @Override
    public StockDeductionResult deductStock(String productId, int quantity) {
        String key = buildKey(productId);

        log.debug("Deducting {} units of stock for product {}", quantity, productId);

        Long result = redisTemplate.execute(
                DEDUCT_STOCK_SCRIPT,
                List.of(key),
                String.valueOf(quantity)
        );

        if (result == null) {
            log.warn("Lua script returned null for product {}", productId);
            return StockDeductionResult.KEY_NOT_FOUND;
        }

        return switch (result.intValue()) {
            case 1 -> {
                log.debug("Stock deduction successful for product {}", productId);
                yield StockDeductionResult.SUCCESS;
            }
            case 0 -> {
                log.debug("Insufficient stock for product {}", productId);
                yield StockDeductionResult.INSUFFICIENT_STOCK;
            }
            default -> {
                log.debug("Stock key not found for product {}", productId);
                yield StockDeductionResult.KEY_NOT_FOUND;
            }
        };
    }

    /**
     * Retrieves the current stock level for a product.
     *
     * @return the stock quantity, or empty if the key does not exist
     */
    @Override
    public Optional<Long> getStock(String productId) {
        String key = buildKey(productId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(value));
    }

    /**
     * Initializes or resets the stock level for a product.
     */
    @Override
    public void setStock(String productId, long quantity) {
        String key = buildKey(productId);
        redisTemplate.opsForValue().set(key, String.valueOf(quantity));
        log.debug("Initialized stock for product {}: {}", productId, quantity);
    }

    private String buildKey(String productId) {
        return KEY_PREFIX + productId;
    }
}
