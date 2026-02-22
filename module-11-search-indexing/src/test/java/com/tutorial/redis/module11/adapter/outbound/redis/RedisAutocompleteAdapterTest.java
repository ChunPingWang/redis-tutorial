package com.tutorial.redis.module11.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module11.domain.model.SearchSuggestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 驗證 RedisAutocompleteAdapter 的自動完成功能整合測試。
 * 測試 RediSearch 的 FT.SUGADD（新增建議）與 FT.SUGGET（查詢建議）指令。
 * 此測試屬於適配器層 (adapter)，確認與 Redis 自動完成字典的互動正確性。
 */
@DisplayName("RedisAutocompleteAdapter 整合測試")
class RedisAutocompleteAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisAutocompleteAdapter autocompleteAdapter;

    // 驗證使用 FT.SUGADD 新增多筆建議後，FT.SUGGET 能依前綴回傳匹配結果
    @Test
    @DisplayName("addAndGetSuggestions_ReturnsMatches — 新增建議後查詢，應回傳匹配的建議")
    void addAndGetSuggestions_ReturnsMatches() {
        // Arrange — add suggestions with different scores
        String dictKey = "autocomplete:products";
        autocompleteAdapter.addSuggestion(dictKey, "wireless headphones", 10.0);
        autocompleteAdapter.addSuggestion(dictKey, "wireless mouse", 5.0);
        autocompleteAdapter.addSuggestion(dictKey, "wired keyboard", 3.0);

        // Act — get suggestions for prefix "wire"
        List<SearchSuggestion> suggestions = autocompleteAdapter.getSuggestions(dictKey, "wire", 5);

        // Assert — should return suggestions matching the prefix
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getSuggestion()).containsIgnoringCase("wire");
    }

    // 驗證當查詢前綴無任何匹配建議時，應回傳空列表
    @Test
    @DisplayName("getSuggestions_NoMatch_ReturnsEmpty — 查詢無匹配前綴時，應回傳空列表")
    void getSuggestions_NoMatch_ReturnsEmpty() {
        // Arrange — add a suggestion that won't match our query prefix
        String dictKey = "autocomplete:nomatch";
        autocompleteAdapter.addSuggestion(dictKey, "wireless headphones", 10.0);

        // Act — get suggestions for prefix that has no matches
        List<SearchSuggestion> suggestions = autocompleteAdapter.getSuggestions(dictKey, "zzz", 5);

        // Assert
        assertThat(suggestions).isEmpty();
    }
}
