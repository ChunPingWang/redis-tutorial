package com.tutorial.redis.module06.application.usecase;

import com.tutorial.redis.module06.domain.model.Order;
import com.tutorial.redis.module06.domain.model.OrderItem;
import com.tutorial.redis.module06.domain.port.outbound.OrderDaoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageOrderService 單元測試")
class ManageOrderServiceTest {

    @Mock
    private OrderDaoPort orderDaoPort;

    @InjectMocks
    private ManageOrderService service;

    @Test
    @DisplayName("createOrder_DelegatesToPort — 建立訂單應委派給 OrderDaoPort.save")
    void createOrder_DelegatesToPort() {
        // Arrange
        List<OrderItem> items = List.of(new OrderItem("p1", "Widget", 1, 10.00));
        Order order = new Order("ord-001", "cust-A", 10.00, "PENDING", items, Instant.now());

        // Act
        service.createOrder(order);

        // Assert
        verify(orderDaoPort, times(1)).save(order);
    }

    @Test
    @DisplayName("findByCustomer_DelegatesToPort — 依客戶查詢應委派給 OrderDaoPort.findByCustomerId")
    void findByCustomer_DelegatesToPort() {
        // Arrange
        List<OrderItem> items = List.of(new OrderItem("p1", "Widget", 1, 10.00));
        List<Order> expected = List.of(
                new Order("ord-ca1", "cust-A", 10.00, "PENDING", items, Instant.now()),
                new Order("ord-ca2", "cust-A", 20.00, "SHIPPED", items, Instant.now())
        );
        when(orderDaoPort.findByCustomerId("cust-A")).thenReturn(expected);

        // Act
        List<Order> result = service.findOrdersByCustomer("cust-A");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Order::getCustomerId)
                .containsOnly("cust-A");
        verify(orderDaoPort, times(1)).findByCustomerId("cust-A");
    }

    @Test
    @DisplayName("findByTimeRange_DelegatesToPort — 依時間範圍查詢應委派給 OrderDaoPort.findByCreatedAtRange")
    void findByTimeRange_DelegatesToPort() {
        // Arrange
        List<OrderItem> items = List.of(new OrderItem("p1", "Gadget", 2, 25.00));
        List<Order> expected = List.of(
                new Order("ord-t1", "cust-B", 50.00, "PENDING", items,
                        Instant.ofEpochMilli(2000))
        );
        when(orderDaoPort.findByCreatedAtRange(1500, 2500)).thenReturn(expected);

        // Act
        List<Order> result = service.findOrdersByTimeRange(1500, 2500);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getOrderId()).isEqualTo("ord-t1");
        verify(orderDaoPort, times(1)).findByCreatedAtRange(1500, 2500);
    }
}
