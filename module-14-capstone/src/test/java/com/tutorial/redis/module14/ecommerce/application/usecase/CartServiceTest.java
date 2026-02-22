package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.model.CartItem;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.CartPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CartService 應用層單元測試類別。
 * 驗證購物車服務的新增品項與取得品項邏輯，使用 Mock 隔離 Redis 依賴。
 * 展示六角形架構中 application usecase 層透過 Port 介面與 adapter 解耦。
 * 所屬：電商子系統 — application 層
 */
@DisplayName("CartService 單元測試")
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartPort cartPort;

    @InjectMocks
    private CartService cartService;

    // 驗證新增購物車品項時，服務層正確委派給 CartPort 並傳入正確的 Key 與資料
    @Test
    @DisplayName("addToCart_DelegatesToPort — 新增購物車品項應委派給 CartPort")
    void addToCart_DelegatesToPort() {
        // Arrange
        CartItem item = new CartItem("p1", "Widget", 10.0, 2);

        // Act
        cartService.addToCart("customer-1", item);

        // Assert — verify addItem was called with the correct cart key and product ID
        verify(cartPort).addItem(eq("ecommerce:cart:customer-1"), eq("p1"), anyString());
    }

    // 驗證從 CartPort 取得原始資料後，服務層能正確解析為 CartItem 領域物件
    @Test
    @DisplayName("getCart_ParsesItemsFromPort — 從 CartPort 取得並解析購物車品項")
    void getCart_ParsesItemsFromPort() {
        // Arrange — return serialized cart items from port
        Map<String, String> rawItems = Map.of(
                "p1", "p1|Widget|10.0|2",
                "p2", "p2|Gadget|25.5|1"
        );
        when(cartPort.getAllItems("ecommerce:cart:customer-1")).thenReturn(rawItems);

        // Act
        List<CartItem> items = cartService.getCart("customer-1");

        // Assert
        assertThat(items).hasSize(2);
        assertThat(items).extracting(CartItem::getProductId)
                .containsExactlyInAnyOrder("p1", "p2");
        verify(cartPort).getAllItems("ecommerce:cart:customer-1");
    }
}
