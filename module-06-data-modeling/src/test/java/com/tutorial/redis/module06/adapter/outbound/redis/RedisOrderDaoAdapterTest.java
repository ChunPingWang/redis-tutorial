package com.tutorial.redis.module06.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module06.domain.model.Order;
import com.tutorial.redis.module06.domain.model.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 RedisOrderDaoAdapter 的訂單 Redis 持久化整合行為。
 * 驗證訂單以 JSON String 模式（內嵌 OrderItem）儲存，搭配客戶 ID 的 Set 二級索引及建立時間的 Sorted Set 時間索引。
 * 屬於 Adapter 層（外部介面卡），示範嵌入式（embedded）反正規化與多種二級索引的資料建模。
 */
@DisplayName("RedisOrderDaoAdapter 整合測試")
class RedisOrderDaoAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisOrderDaoAdapter adapter;

    // 驗證訂單（含內嵌品項）以 JSON 序列化存入 Redis 後，可透過主鍵查回完整物件
    @Test
    @DisplayName("save_AndFindById_ReturnsOrder — 儲存含 2 個品項的訂單後以 ID 查詢，應回傳完整物件")
    void save_AndFindById_ReturnsOrder() {
        // Arrange
        List<OrderItem> items = List.of(
                new OrderItem("prod-A", "Keyboard", 2, 49.99),
                new OrderItem("prod-B", "Mouse", 1, 29.99)
        );
        Instant createdAt = Instant.ofEpochMilli(1700000000000L);
        Order order = new Order("ord-001", "cust-A", 129.97, "PENDING", items, createdAt);

        // Act
        adapter.save(order);
        Optional<Order> found = adapter.findById("ord-001");

        // Assert
        assertThat(found).isPresent();
        Order result = found.get();
        assertThat(result.getOrderId()).isEqualTo("ord-001");
        assertThat(result.getCustomerId()).isEqualTo("cust-A");
        assertThat(result.getTotalAmount()).isEqualTo(129.97);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Keyboard");
        assertThat(result.getItems().get(1).getProductName()).isEqualTo("Mouse");
    }

    // 驗證查詢不存在的訂單 ID 時，回傳空的 Optional
    @Test
    @DisplayName("findById_WhenNotExists_ReturnsEmpty — 查詢不存在的訂單，應回傳空 Optional")
    void findById_WhenNotExists_ReturnsEmpty() {
        // Act
        Optional<Order> result = adapter.findById("non-existent-order");

        // Assert
        assertThat(result).isEmpty();
    }

    // 驗證刪除訂單時，主 Key、客戶 ID 索引及時間索引同步清除，避免孤立索引
    @Test
    @DisplayName("delete_RemovesOrderAndIndexes — 刪除訂單後，實體與所有二級索引都應被清除")
    void delete_RemovesOrderAndIndexes() {
        // Arrange
        List<OrderItem> items = List.of(
                new OrderItem("prod-X", "Monitor", 1, 399.99)
        );
        Instant createdAt = Instant.ofEpochMilli(1700000000000L);
        Order order = new Order("ord-del", "cust-del", 399.99, "PENDING", items, createdAt);
        adapter.save(order);

        // Verify saved
        assertThat(adapter.findById("ord-del")).isPresent();
        assertThat(adapter.findByCustomerId("cust-del")).hasSize(1);
        assertThat(adapter.findByCreatedAtRange(1699999999000L, 1700000001000L)).hasSize(1);

        // Act
        adapter.delete("ord-del");

        // Assert — entity removed
        assertThat(adapter.findById("ord-del")).isEmpty();

        // Assert — indexes cleaned
        assertThat(adapter.findByCustomerId("cust-del")).isEmpty();
        assertThat(adapter.findByCreatedAtRange(1699999999000L, 1700000001000L)).isEmpty();
    }

    // 驗證透過客戶 ID 的 Set 二級索引查詢，回傳該客戶的所有訂單
    @Test
    @DisplayName("findByCustomerId_ReturnsCustomerOrders — 3 筆訂單 (2 cust-A, 1 cust-B)，查詢 cust-A 應回傳 2 筆")
    void findByCustomerId_ReturnsCustomerOrders() {
        // Arrange
        List<OrderItem> items = List.of(new OrderItem("prod-1", "Widget", 1, 10.00));

        adapter.save(new Order("ord-ca-1", "cust-A", 10.00, "PENDING", items,
                Instant.ofEpochMilli(1700000001000L)));
        adapter.save(new Order("ord-ca-2", "cust-A", 20.00, "SHIPPED", items,
                Instant.ofEpochMilli(1700000002000L)));
        adapter.save(new Order("ord-cb-1", "cust-B", 30.00, "PENDING", items,
                Instant.ofEpochMilli(1700000003000L)));

        // Act
        List<Order> custAOrders = adapter.findByCustomerId("cust-A");
        List<Order> custBOrders = adapter.findByCustomerId("cust-B");

        // Assert
        assertThat(custAOrders).hasSize(2);
        assertThat(custAOrders).extracting(Order::getOrderId)
                .containsExactlyInAnyOrder("ord-ca-1", "ord-ca-2");

        assertThat(custBOrders).hasSize(1);
        assertThat(custBOrders.getFirst().getOrderId()).isEqualTo("ord-cb-1");
    }

    // 驗證透過 Sorted Set 時間索引做範圍查詢，回傳指定時間區間內的訂單
    @Test
    @DisplayName("findByCreatedAtRange_ReturnsOrdersInRange — 3 筆訂單時間 1000/2000/3000，查詢 1500~2500 應回傳 1 筆")
    void findByCreatedAtRange_ReturnsOrdersInRange() {
        // Arrange
        List<OrderItem> items = List.of(new OrderItem("prod-1", "Gadget", 1, 15.00));

        adapter.save(new Order("ord-t1", "cust-X", 15.00, "PENDING", items,
                Instant.ofEpochMilli(1000)));
        adapter.save(new Order("ord-t2", "cust-X", 25.00, "PENDING", items,
                Instant.ofEpochMilli(2000)));
        adapter.save(new Order("ord-t3", "cust-X", 35.00, "PENDING", items,
                Instant.ofEpochMilli(3000)));

        // Act
        List<Order> result = adapter.findByCreatedAtRange(1500, 2500);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getOrderId()).isEqualTo("ord-t2");
        assertThat(result.getFirst().getCreatedAt()).isEqualTo(Instant.ofEpochMilli(2000));
    }
}
