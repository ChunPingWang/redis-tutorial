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

@DisplayName("JsonDocumentService 單元測試")
@ExtendWith(MockitoExtension.class)
class JsonDocumentServiceTest {

    @Mock
    private JsonDocumentPort jsonDocumentPort;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JsonDocumentService service;

    @Test
    @DisplayName("deleteProduct_DelegatesToPort — 刪除商品應委派給 JsonDocumentPort.deleteDocument")
    void deleteProduct_DelegatesToPort() {
        // Act — delete product P001
        service.deleteProduct("P001");

        // Assert — should delegate to port with the correct key
        verify(jsonDocumentPort).deleteDocument("product:P001");
    }

    @Test
    @DisplayName("updatePrice_CallsSetDocument — 更新價格應呼叫 setDocument 設定 $.price")
    void updatePrice_CallsSetDocument() {
        // Act — update the price of product P001 to 129.99
        service.updatePrice("P001", 129.99);

        // Assert — should call setDocument with the price path and new value
        verify(jsonDocumentPort).setDocument(eq("product:P001"), eq("$.price"), eq("129.99"));
    }
}
