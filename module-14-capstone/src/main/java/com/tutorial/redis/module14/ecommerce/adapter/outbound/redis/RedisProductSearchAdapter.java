package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.module14.ecommerce.domain.port.outbound.ProductSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Redis adapter for product search and autocomplete operations.
 *
 * <p>Implements {@link ProductSearchPort} using RediSearch module commands
 * executed via Lua scripts. Products are stored as Redis Hashes with prefix
 * {@code ecommerce:product:} and indexed for full-text search. Autocomplete
 * suggestions use the {@code FT.SUGADD}/{@code FT.SUGGET} commands.</p>
 *
 * <p>Index name: {@code idx:ecommerce-products}</p>
 * <p>Suggestion key: {@code ecommerce:suggest:products}</p>
 */
@Component
public class RedisProductSearchAdapter implements ProductSearchPort {

    private static final Logger log = LoggerFactory.getLogger(RedisProductSearchAdapter.class);
    private static final String PRODUCT_KEY_PREFIX = "ecommerce:product:";
    private static final String INDEX_NAME = "idx:ecommerce-products";
    private static final String SUGGESTION_KEY = "ecommerce:suggest:products";

    private static final DefaultRedisScript<String> CREATE_INDEX_SCRIPT = new DefaultRedisScript<>(
            "local ok, err = pcall(redis.call, 'FT.CREATE', '" + INDEX_NAME + "', " +
            "'ON', 'HASH', 'PREFIX', '1', '" + PRODUCT_KEY_PREFIX + "', " +
            "'SCHEMA', 'name', 'TEXT', 'SORTABLE', 'category', 'TAG', " +
            "'price', 'NUMERIC', 'SORTABLE', 'description', 'TEXT')\n" +
            "if ok then return 'OK' else return tostring(err) end",
            String.class);

    private static final DefaultRedisScript<String> SEARCH_SCRIPT = new DefaultRedisScript<>(
            "local r = redis.call('FT.SEARCH', '" + INDEX_NAME + "', ARGV[1], 'LIMIT', '0', '10')\n" +
            "return cjson.encode(r)",
            String.class);

    private static final DefaultRedisScript<String> SUGADD_SCRIPT = new DefaultRedisScript<>(
            "redis.call('FT.SUGADD', KEYS[1], ARGV[1], ARGV[2])\n" +
            "return 'OK'",
            String.class);

    private static final DefaultRedisScript<String> SUGGET_SCRIPT = new DefaultRedisScript<>(
            "local r = redis.call('FT.SUGGET', KEYS[1], ARGV[1], 'FUZZY', 'MAX', '5')\n" +
            "if r == nil or r == false then return '[]' end\n" +
            "return cjson.encode(r)",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisProductSearchAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void indexProduct(String productId, Map<String, String> fields) {
        log.debug("Indexing product {} with fields {}", productId, fields.keySet());

        // Store product as hash
        String hashKey = PRODUCT_KEY_PREFIX + productId;
        stringRedisTemplate.opsForHash().putAll(hashKey, fields);

        // Ensure search index exists (idempotent via pcall)
        try {
            stringRedisTemplate.execute(CREATE_INDEX_SCRIPT, Collections.emptyList());
        } catch (Exception e) {
            log.debug("Index creation result: {}", e.getMessage());
        }
    }

    @Override
    public List<String> search(String query) {
        log.debug("Searching products with query: {}", query);
        try {
            String json = stringRedisTemplate.execute(SEARCH_SCRIPT,
                    Collections.emptyList(), query);
            if (json == null || json.isEmpty()) {
                return Collections.emptyList();
            }
            return parseSearchResults(json);
        } catch (Exception e) {
            log.warn("Search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void addSuggestion(String term, double score) {
        log.debug("Adding suggestion: {} with score {}", term, score);
        try {
            stringRedisTemplate.execute(SUGADD_SCRIPT,
                    List.of(SUGGESTION_KEY), term, String.valueOf(score));
        } catch (Exception e) {
            log.warn("Failed to add suggestion: {}", e.getMessage());
        }
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        log.debug("Getting suggestions for prefix: {}", prefix);
        try {
            String json = stringRedisTemplate.execute(SUGGET_SCRIPT,
                    List.of(SUGGESTION_KEY), prefix);
            if (json == null || json.isEmpty() || "[]".equals(json.trim())) {
                return Collections.emptyList();
            }
            return parseJsonStringArray(json);
        } catch (Exception e) {
            log.warn("Failed to get suggestions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parses FT.SEARCH results from cjson.encode format.
     *
     * <p>The result is a JSON array where the first element is the total count,
     * followed by alternating key name and field array pairs:
     * {@code [count, "key1", ["field1", "value1", ...], "key2", [...], ...]}</p>
     *
     * <p>Extracts just the key names (stripping the hash key prefix).</p>
     */
    private List<String> parseSearchResults(String json) {
        List<String> results = new ArrayList<>();
        try {
            // Remove outer brackets
            String trimmed = json.trim();
            if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                return results;
            }
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            if (trimmed.isEmpty()) {
                return results;
            }

            // Split at top level, respecting nested arrays
            List<String> elements = splitTopLevelElements(trimmed);

            // First element is the count, then alternating key/fields pairs
            for (int i = 1; i < elements.size(); i += 2) {
                String keyElement = elements.get(i).trim();
                // Remove quotes
                if (keyElement.startsWith("\"") && keyElement.endsWith("\"")) {
                    keyElement = keyElement.substring(1, keyElement.length() - 1);
                }
                // Strip prefix to get product ID
                if (keyElement.startsWith(PRODUCT_KEY_PREFIX)) {
                    results.add(keyElement.substring(PRODUCT_KEY_PREFIX.length()));
                } else {
                    results.add(keyElement);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse search results: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Splits a JSON value into top-level elements, respecting nested brackets.
     */
    private List<String> splitTopLevelElements(String content) {
        List<String> elements = new ArrayList<>();
        int depth = 0;
        int start = 0;
        boolean inString = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '[' || c == '{') {
                    depth++;
                } else if (c == ']' || c == '}') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    elements.add(content.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        if (start < content.length()) {
            elements.add(content.substring(start).trim());
        }
        return elements;
    }

    /**
     * Parses a JSON string array like {@code ["term1","term2"]}.
     */
    private List<String> parseJsonStringArray(String json) {
        List<String> results = new ArrayList<>();
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return results;
        }
        trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        if (trimmed.isEmpty()) {
            return results;
        }
        for (String part : trimmed.split(",")) {
            String stripped = part.trim();
            if (stripped.startsWith("\"") && stripped.endsWith("\"")) {
                stripped = stripped.substring(1, stripped.length() - 1);
            }
            if (!stripped.isEmpty()) {
                results.add(stripped);
            }
        }
        return results;
    }
}
