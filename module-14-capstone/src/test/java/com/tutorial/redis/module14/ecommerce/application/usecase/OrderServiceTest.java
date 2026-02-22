package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.model.CartItem;
import com.tutorial.redis.module14.ecommerce.domain.model.Order;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.OrderStreamPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

/**
 * OrderService 應用層單元測試類別。
 * 驗證建立訂單時正確將訂單事件發布至 Redis Stream。
 * 展示事件驅動架構中使用 Redis Stream 作為訊息通道。
 * 所屬：電商子系統 — application 層
 */
@DisplayName("OrderService 單元測試")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderStreamPort orderStreamPort;

    @InjectMocks
    private OrderService orderService;

    // 驗證建立訂單時，服務層透過 OrderStreamPort 將訂單發布至 Stream
    @Test
    @DisplayName("createOrder_PublishesToStream — 建立訂單應發布至 Redis Stream")
    void createOrder_PublishesToStream() {
        // Arrange
        Order order = new Order("order-1", "customer-1",
                List.of(new CartItem("p1", "Widget", 10.0, 2)),
                20.0, "PENDING", System.currentTimeMillis());

        // Act
        orderService.createOrder(order);

        // Assert — verify the order was published to the stream port
        verify(orderStreamPort).publishOrder(order);
    }
}
