package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.port.outbound.ProductTagPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品標籤管理 Use Case 單元測試
 * 驗證 ManageProductTagService 正確委派操作至 ProductTagPort（Redis Set）。
 * 層級：Application（Use Case 業務邏輯）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManageProductTagService 單元測試")
class ManageProductTagServiceTest {

    @Mock
    private ProductTagPort productTagPort;

    @InjectMocks
    private ManageProductTagService service;

    // 驗證新增標籤時正確委派至 Port 的 addTags
    @Test
    @DisplayName("tagProduct_DelegatesToPort — 委派至 Port 的 addTags 方法")
    void tagProduct_DelegatesToPort() {
        Set<String> tags = Set.of("java", "spring");

        service.tagProduct("PROD-001", tags);

        verify(productTagPort).addTags("PROD-001", tags);
    }

    // 驗證移除標籤時正確委派至 Port 的 removeTags
    @Test
    @DisplayName("untagProduct_DelegatesToPort — 委派至 Port 的 removeTags 方法")
    void untagProduct_DelegatesToPort() {
        Set<String> tags = Set.of("java");

        service.untagProduct("PROD-001", tags);

        verify(productTagPort).removeTags("PROD-001", tags);
    }

    // 驗證取得標籤時正確委派至 Port 的 getTags，並回傳結果
    @Test
    @DisplayName("getProductTags_DelegatesToPort — 委派至 Port 的 getTags 方法")
    void getProductTags_DelegatesToPort() {
        when(productTagPort.getTags("PROD-001")).thenReturn(Set.of("java", "spring"));

        Set<String> result = service.getProductTags("PROD-001");

        assertThat(result).containsExactlyInAnyOrder("java", "spring");
        verify(productTagPort).getTags("PROD-001");
    }

    // 驗證查詢共同標籤時正確委派至 Port 的 getCommonTags
    @Test
    @DisplayName("findCommonTags_DelegatesToPort — 委派至 Port 的 getCommonTags 方法")
    void findCommonTags_DelegatesToPort() {
        when(productTagPort.getCommonTags("P-001", "P-002")).thenReturn(Set.of("spring"));

        Set<String> result = service.findCommonTags("P-001", "P-002");

        assertThat(result).containsExactly("spring");
        verify(productTagPort).getCommonTags("P-001", "P-002");
    }
}
