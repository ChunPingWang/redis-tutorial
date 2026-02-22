package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.model.CartItem;
import com.tutorial.redis.module02.domain.model.ShoppingCart;
import com.tutorial.redis.module02.domain.port.outbound.ShoppingCartPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 購物車管理 Use Case 單元測試
 * 驗證 ManageCartService 正確委派操作至 ShoppingCartPort（Redis Hash）。
 * 層級：Application（Use Case 業務邏輯）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManageCartService 單元測試")
class ManageCartServiceTest {

    @Mock
    private ShoppingCartPort shoppingCartPort;

    @InjectMocks
    private ManageCartService service;

    // 驗證新增商品至購物車時正確委派至 Port 的 addItem
    @Test
    @DisplayName("addToCart_DelegatesToPort — 委派至 Port 的 addItem 方法")
    void addToCart_DelegatesToPort() {
        CartItem item = new CartItem("P-001", "Redis Book", new BigDecimal("29.99"), 1);

        service.addToCart("CUST-001", item);

        verify(shoppingCartPort).addItem("CUST-001", item);
    }

    // 驗證移除購物車商品時正確委派至 Port 的 removeItem
    @Test
    @DisplayName("removeFromCart_DelegatesToPort — 委派至 Port 的 removeItem 方法")
    void removeFromCart_DelegatesToPort() {
        service.removeFromCart("CUST-001", "P-001");

        verify(shoppingCartPort).removeItem("CUST-001", "P-001");
    }

    // 驗證取得購物車時正確委派至 Port 的 getCart，並回傳結果
    @Test
    @DisplayName("getCart_DelegatesToPort — 委派至 Port 的 getCart 方法")
    void getCart_DelegatesToPort() {
        ShoppingCart cart = new ShoppingCart("CUST-001", new LinkedHashMap<>());
        when(shoppingCartPort.getCart("CUST-001")).thenReturn(Optional.of(cart));

        Optional<ShoppingCart> result = service.getCart("CUST-001");

        assertThat(result).isPresent();
        assertThat(result.get().getCustomerId()).isEqualTo("CUST-001");
        verify(shoppingCartPort).getCart("CUST-001");
    }

    // 驗證清空購物車時正確委派至 Port 的 deleteCart
    @Test
    @DisplayName("clearCart_DelegatesToPort — 委派至 Port 的 deleteCart 方法")
    void clearCart_DelegatesToPort() {
        service.clearCart("CUST-001");

        verify(shoppingCartPort).deleteCart("CUST-001");
    }
}
