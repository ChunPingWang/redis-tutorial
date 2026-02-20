package com.tutorial.redis.module11.adapter.outbound.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.redis.module11.domain.model.SearchSuggestion;
import com.tutorial.redis.module11.domain.port.outbound.AutocompletePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Redis adapter for RediSearch autocomplete operations using Lua scripts.
 *
 * <p>Invokes FT.SUGADD and FT.SUGGET via {@link DefaultRedisScript}
 * because {@code connection.execute()} is not supported for module commands
 * in Lettuce / Spring Data Redis 4.x.</p>
 *
 * <p>FT.SUGGET with WITHSCORES returns pairs: [suggestion1, score1, suggestion2, score2, ...]
 * which are serialized via {@code cjson.encode()} in Lua and parsed in Java.</p>
 */
@Component
public class RedisAutocompleteAdapter implements AutocompletePort {

    private static final Logger log = LoggerFactory.getLogger(RedisAutocompleteAdapter.class);

    private static final DefaultRedisScript<String> FT_SUGADD = new DefaultRedisScript<>(
            "return tostring(redis.call('FT.SUGADD', KEYS[1], ARGV[1], ARGV[2]))",
            String.class);

    private static final DefaultRedisScript<String> FT_SUGGET = new DefaultRedisScript<>(
            "local result = redis.call('FT.SUGGET', KEYS[1], ARGV[1], 'MAX', ARGV[2], 'WITHSCORES') " +
                    "if result == nil or result == false then return '[]' end " +
                    "return cjson.encode(result)",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAutocompleteAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void addSuggestion(String dictionaryKey, String suggestion, double score) {
        stringRedisTemplate.execute(FT_SUGADD, List.of(dictionaryKey),
                suggestion, String.valueOf(score));
        log.debug("Added suggestion '{}' with score {} to dictionary '{}'",
                suggestion, score, dictionaryKey);
    }

    @Override
    public List<SearchSuggestion> getSuggestions(String dictionaryKey, String prefix, int maxResults) {
        String json = stringRedisTemplate.execute(FT_SUGGET, List.of(dictionaryKey),
                prefix, String.valueOf(maxResults));
        return parseSuggestions(json);
    }

    /**
     * Parses the cjson-encoded FT.SUGGET response into a list of {@link SearchSuggestion}.
     *
     * <p>FT.SUGGET with WITHSCORES returns pairs:
     * {@code [suggestion1, score1, suggestion2, score2, ...]}</p>
     *
     * @param json the cjson-encoded response string
     * @return list of parsed suggestions
     */
    private List<SearchSuggestion> parseSuggestions(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return Collections.emptyList();
        }

        try {
            List<?> rawArray = objectMapper.readValue(json, List.class);
            if (rawArray == null || rawArray.isEmpty()) {
                return Collections.emptyList();
            }

            List<SearchSuggestion> suggestions = new ArrayList<>();

            // Pairs: [suggestion1, score1, suggestion2, score2, ...]
            for (int i = 0; i < rawArray.size() - 1; i += 2) {
                String suggestion = String.valueOf(rawArray.get(i));
                double score = toDouble(rawArray.get(i + 1));
                suggestions.add(new SearchSuggestion(suggestion, score));
            }

            return suggestions;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse FT.SUGGET JSON response: {}", json, e);
            return Collections.emptyList();
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
