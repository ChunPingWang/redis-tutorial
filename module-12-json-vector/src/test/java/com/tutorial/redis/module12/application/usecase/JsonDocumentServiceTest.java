package com.tutorial.redis.module12.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.redis.module12.domain.port.outbound.JsonDocumentPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * JsonDocumentService 單元測試，驗證 Application 層的 JSON 文件業務邏輯。
 * 使用 Mock 確認 Service 正確委派 RedisJSON 操作（如 JSON.SET 設定路徑值、
 * 刪除文件等）給底層的 JsonDocumentPort。
 * 屬於 Application（應用服務）層的測試。
 */
@DisplayName("JsonDocumentService 單元測試")
@ExtendWith(MockitoExtension.class)
class JsonDocumentServiceTest {

    @Mock
    private JsonDocumentPort jsonDocumentPort;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JsonDocumentService service;

    // 驗證刪除商品時，Service 正確委派給 Port 並帶入正確的 key
    @Test
    @DisplayName("deleteProduct_DelegatesToPort — 刪除商品應委派給 JsonDocumentPort.deleteDocument")
    void deleteProduct_DelegatesToPort() {
        // Act — delete product P001
        service.deleteProduct("P001");

        // Assert — should delegate to port with the correct key
        verify(jsonDocumentPort).deleteDocument("product:P001");
    }

    // 驗證更新價格時，Service 透過 JSON.SET 設定 $.price 路徑的值
    @Test
    @DisplayName("updatePrice_CallsSetDocument — 更新價格應呼叫 setDocument 設定 $.price")
    void updatePrice_CallsSetDocument() {
        // Act — update the price of product P001 to 129.99
        service.updatePrice("P001", 129.99);

        // Assert — should call setDocument with the price path and new value
        verify(jsonDocumentPort).setDocument(eq("product:P001"), eq("$.price"), eq("129.99"));
    }
}
