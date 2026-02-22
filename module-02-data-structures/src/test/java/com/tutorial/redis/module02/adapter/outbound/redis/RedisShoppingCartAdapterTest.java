package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module02.domain.model.CartItem;
import com.tutorial.redis.module02.domain.model.ShoppingCart;
import com.tutorial.redis.module02.domain.port.outbound.ShoppingCartPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 購物車 Adapter 整合測試
 * 驗證使用 Redis Hash（HSET/HGET/HDEL/HGETALL）實作購物車的 CRUD 操作。
 * 層級：Adapter（外部端口實作）
 */
@DisplayName("RedisShoppingCartAdapter 整合測試")
class RedisShoppingCartAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private ShoppingCartPort shoppingCartPort;

    private CartItem createCartItem(String productId, String name, String price, int qty) {
        return new CartItem(productId, name, new BigDecimal(price), qty);
    }

    // 驗證 HSET 將商品新增至空購物車，並能透過 HGET 正確讀取
    @Test
    @DisplayName("addItem_WhenNewCart_CreatesCartWithItem — 新增商品到空購物車")
    void addItem_WhenNewCart_CreatesCartWithItem() {
        CartItem item = createCartItem("P-001", "Redis Book", "29.99", 1);

        shoppingCartPort.addItem("CUST-001", item);

        assertThat(shoppingCartPort.cartExists("CUST-001")).isTrue();
        Optional<CartItem> retrieved = shoppingCartPort.getItem("CUST-001", "P-001");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProductName()).isEqualTo("Redis Book");
        assertThat(retrieved.get().getUnitPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
    }

    // 驗證對已有商品的購物車再次 HSET 新增不同商品
    @Test
    @DisplayName("addItem_WhenExistingCart_AddsToCart — 新增商品到已有購物車")
    void addItem_WhenExistingCart_AddsToCart() {
        shoppingCartPort.addItem("CUST-002", createCartItem("P-001", "Redis Book", "29.99", 1));
        shoppingCartPort.addItem("CUST-002", createCartItem("P-002", "Spring Guide", "39.99", 2));

        Optional<ShoppingCart> cart = shoppingCartPort.getCart("CUST-002");
        assertThat(cart).isPresent();
        assertThat(cart.get().itemCount()).isEqualTo(2);
        assertThat(cart.get().getItem("P-001")).isPresent();
        assertThat(cart.get().getItem("P-002")).isPresent();
    }

    // 驗證 HDEL 能移除購物車中的指定商品，其他商品不受影響
    @Test
    @DisplayName("removeItem_WhenItemExists_RemovesFromCart — 移除購物車中的商品")
    void removeItem_WhenItemExists_RemovesFromCart() {
        shoppingCartPort.addItem("CUST-003", createCartItem("P-001", "Redis Book", "29.99", 1));
        shoppingCartPort.addItem("CUST-003", createCartItem("P-002", "Spring Guide", "39.99", 2));

        shoppingCartPort.removeItem("CUST-003", "P-001");

        Optional<CartItem> removed = shoppingCartPort.getItem("CUST-003", "P-001");
        assertThat(removed).isEmpty();
        Optional<CartItem> remaining = shoppingCartPort.getItem("CUST-003", "P-002");
        assertThat(remaining).isPresent();
    }

    // 驗證透過 HSET 部分更新（partial update）商品數量
    @Test
    @DisplayName("updateItemQuantity_WhenItemExists_UpdatesQuantity — 更新購物車商品數量")
    void updateItemQuantity_WhenItemExists_UpdatesQuantity() {
        shoppingCartPort.addItem("CUST-004", createCartItem("P-001", "Redis Book", "29.99", 1));

        shoppingCartPort.updateItemQuantity("CUST-004", "P-001", 5);

        Optional<CartItem> updated = shoppingCartPort.getItem("CUST-004", "P-001");
        assertThat(updated).isPresent();
        assertThat(updated.get().getQuantity()).isEqualTo(5);
    }

    // 驗證 HGETALL 能取得購物車的完整商品資料
    @Test
    @DisplayName("getCart_WhenMultipleItems_ReturnsFullCart — 取得完整購物車資料")
    void getCart_WhenMultipleItems_ReturnsFullCart() {
        shoppingCartPort.addItem("CUST-005", createCartItem("P-001", "Redis Book", "29.99", 1));
        shoppingCartPort.addItem("CUST-005", createCartItem("P-002", "Spring Guide", "39.99", 2));
        shoppingCartPort.addItem("CUST-005", createCartItem("P-003", "Docker Manual", "19.99", 3));

        Optional<ShoppingCart> cart = shoppingCartPort.getCart("CUST-005");

        assertThat(cart).isPresent();
        assertThat(cart.get().getCustomerId()).isEqualTo("CUST-005");
        assertThat(cart.get().itemCount()).isEqualTo(3);
        assertThat(cart.get().getItems()).containsKeys("P-001", "P-002", "P-003");
    }

    // 驗證 DEL 指令能完全刪除購物車 Hash key
    @Test
    @DisplayName("deleteCart_WhenCartExists_RemovesAll — 清空購物車")
    void deleteCart_WhenCartExists_RemovesAll() {
        shoppingCartPort.addItem("CUST-006", createCartItem("P-001", "Redis Book", "29.99", 1));
        shoppingCartPort.addItem("CUST-006", createCartItem("P-002", "Spring Guide", "39.99", 2));

        shoppingCartPort.deleteCart("CUST-006");

        assertThat(shoppingCartPort.cartExists("CUST-006")).isFalse();
        assertThat(shoppingCartPort.getCart("CUST-006")).isEmpty();
    }

    // 驗證 Redis key 遵循 ecommerce:cart:{customerId} 命名規範
    @Test
    @DisplayName("key_FollowsNamingConvention — 驗證 key 符合 ecommerce:cart:* 命名規範")
    void key_FollowsNamingConvention() {
        shoppingCartPort.addItem("CUST-KEY-001", createCartItem("P-001", "Redis Book", "29.99", 1));

        Set<String> keys = stringRedisTemplate.keys("ecommerce:cart:CUST-KEY-001");
        assertThat(keys).isNotNull().hasSize(1);
        assertThat(keys.iterator().next()).isEqualTo("ecommerce:cart:CUST-KEY-001");
    }
}
