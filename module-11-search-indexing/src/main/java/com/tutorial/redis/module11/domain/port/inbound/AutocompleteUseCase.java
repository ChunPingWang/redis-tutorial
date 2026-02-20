package com.tutorial.redis.module11.domain.port.inbound;

import com.tutorial.redis.module11.domain.model.SearchSuggestion;

import java.util.List;
import java.util.Map;

/**
 * Inbound port for autocomplete (type-ahead) operations.
 *
 * <p>Provides use cases for populating and querying RediSearch
 * suggestion dictionaries (FT.SUGADD / FT.SUGGET).</p>
 */
public interface AutocompleteUseCase {

    /**
     * Adds multiple suggestions to the autocomplete dictionary in bulk.
     *
     * @param dictionaryKey the suggestion dictionary key
     * @param suggestions   map of suggestion text to score
     */
    void addSuggestions(String dictionaryKey, Map<String, Double> suggestions);

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
