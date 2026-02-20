package com.tutorial.redis.module11.application.usecase;

import com.tutorial.redis.module11.domain.model.SearchSuggestion;
import com.tutorial.redis.module11.domain.port.inbound.AutocompleteUseCase;
import com.tutorial.redis.module11.domain.port.outbound.AutocompletePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Application service implementing RediSearch autocomplete use cases.
 *
 * <p>Delegates to {@link AutocompletePort} for FT.SUGADD and FT.SUGGET operations.
 * Demonstrates populating suggestion dictionaries and retrieving
 * prefix-based autocomplete suggestions.</p>
 */
@Service
public class AutocompleteService implements AutocompleteUseCase {

    private static final Logger log = LoggerFactory.getLogger(AutocompleteService.class);

    private final AutocompletePort autocompletePort;

    public AutocompleteService(AutocompletePort autocompletePort) {
        this.autocompletePort = autocompletePort;
    }

    @Override
    public void addSuggestions(String dictionaryKey, Map<String, Double> suggestions) {
        for (Map.Entry<String, Double> entry : suggestions.entrySet()) {
            autocompletePort.addSuggestion(dictionaryKey, entry.getKey(), entry.getValue());
        }
        log.info("Added {} suggestions to dictionary '{}'", suggestions.size(), dictionaryKey);
    }

    @Override
    public List<SearchSuggestion> getSuggestions(String dictionaryKey, String prefix, int maxResults) {
        return autocompletePort.getSuggestions(dictionaryKey, prefix, maxResults);
    }
}
