package com.tutorial.redis.module11.domain.port.outbound;

import com.tutorial.redis.module11.domain.model.SearchSuggestion;

import java.util.List;

/**
 * Outbound port for RediSearch autocomplete operations (FT.SUGADD, FT.SUGGET).
 *
 * <p>Implemented by Redis adapter using Lua scripts (DefaultRedisScript) to invoke
 * RediSearch suggestion commands.</p>
 */
public interface AutocompletePort {

    /**
     * Adds a suggestion to the autocomplete dictionary.
     *
     * @param dictionaryKey the suggestion dictionary key
     * @param suggestion    the suggestion text
     * @param score         the suggestion score (higher = more relevant)
     */
    void addSuggestion(String dictionaryKey, String suggestion, double score);

    /**
     * Retrieves autocomplete suggestions matching the given prefix.
     *
     * @param dictionaryKey the suggestion dictionary key
     * @param prefix        the prefix to match against
     * @param maxResults    maximum number of suggestions to return
     * @return list of matching suggestions ordered by score
     */
    List<SearchSuggestion> getSuggestions(String dictionaryKey, String prefix, int maxResults);
}
