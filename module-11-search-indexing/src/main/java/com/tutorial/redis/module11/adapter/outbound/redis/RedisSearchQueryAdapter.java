package com.tutorial.redis.module11.adapter.outbound.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.redis.module11.domain.model.AggregationResult;
import com.tutorial.redis.module11.domain.model.SearchResult;
import com.tutorial.redis.module11.domain.port.outbound.SearchQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis adapter for RediSearch query operations using Lua scripts.
 *
 * <p>Invokes FT.SEARCH and FT.AGGREGATE via {@link DefaultRedisScript}.
 * Complex Redis array responses are serialized to JSON within Lua using
 * {@code cjson.encode()}, then parsed in Java with Jackson {@link ObjectMapper}.</p>
 *
 * <p>FT.SEARCH returns: [totalResults, docId1, [field1, value1, ...], docId2, [field2, value2, ...], ...]
 * After cjson.encode, this becomes a JSON array that is parsed into a {@link SearchResult}.</p>
 */
@Component
public class RedisSearchQueryAdapter implements SearchQueryPort {

    private static final Logger log = LoggerFactory.getLogger(RedisSearchQueryAdapter.class);

    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 10;

    private static final DefaultRedisScript<String> FT_SEARCH = new DefaultRedisScript<>(
            "local result = redis.call('FT.SEARCH', KEYS[1], ARGV[1], 'LIMIT', ARGV[2], ARGV[3]) " +
                    "return cjson.encode(result)",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSearchQueryAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public SearchResult search(String indexName, String query) {
        return search(indexName, query, DEFAULT_OFFSET, DEFAULT_LIMIT);
    }

    @Override
    public SearchResult search(String indexName, String query, int offset, int limit) {
        String json = stringRedisTemplate.execute(FT_SEARCH, List.of(indexName),
                query, String.valueOf(offset), String.valueOf(limit));
        return parseSearchResult(json);
    }

    @Override
    public AggregationResult aggregate(String indexName, String query, List<String> aggregationArgs) {
        String luaScript = buildAggregateScript(aggregationArgs.size());
        DefaultRedisScript<String> script = new DefaultRedisScript<>(luaScript, String.class);

        List<String> args = new ArrayList<>();
        args.add(query);
        args.addAll(aggregationArgs);

        String json = stringRedisTemplate.execute(script, List.of(indexName),
                (Object[]) args.toArray(new String[0]));
        return parseAggregationResult(json);
    }

    /**
     * Builds a Lua script for FT.AGGREGATE that dynamically constructs the command.
     *
     * <p>ARGV[1] is the query, ARGV[2..N] are the aggregation arguments
     * (e.g., GROUPBY, 1, @category, REDUCE, AVG, 1, @price, AS, avg_price).</p>
     *
     * @param aggregationArgCount number of aggregation arguments (not counting query)
     * @return the Lua script text
     */
    private String buildAggregateScript(int aggregationArgCount) {
        StringBuilder lua = new StringBuilder();
        lua.append("local args = {'FT.AGGREGATE', KEYS[1], ARGV[1]} ");
        lua.append("for i = 2, #ARGV do ");
        lua.append("args[#args + 1] = ARGV[i] ");
        lua.append("end ");
        lua.append("local result = redis.call(unpack(args)) ");
        lua.append("return cjson.encode(result)");
        return lua.toString();
    }

    /**
     * Parses the cjson-encoded FT.SEARCH response into a {@link SearchResult}.
     *
     * <p>The JSON array format is:
     * {@code [totalResults, docId1, [field1, value1, ...], docId2, [field2, value2, ...], ...]}</p>
     *
     * @param json the cjson-encoded response string
     * @return the parsed search result
     */
    private SearchResult parseSearchResult(String json) {
        if (json == null || json.isEmpty()) {
            return new SearchResult(0, Collections.emptyList());
        }

        try {
            List<?> rawArray = objectMapper.readValue(json, List.class);
            if (rawArray == null || rawArray.isEmpty()) {
                return new SearchResult(0, Collections.emptyList());
            }

            long totalResults = toLong(rawArray.get(0));
            List<Map<String, String>> documents = new ArrayList<>();

            // FT.SEARCH returns: [total, docId1, [f1, v1, f2, v2, ...], docId2, [f1, v1, ...], ...]
            // After cjson.encode: each field array becomes a JSON array
            for (int i = 1; i < rawArray.size(); i += 2) {
                if (i + 1 >= rawArray.size()) {
                    break;
                }

                String docId = String.valueOf(rawArray.get(i));
                Object fieldsObj = rawArray.get(i + 1);

                Map<String, String> doc = new LinkedHashMap<>();
                doc.put("_id", docId);

                if (fieldsObj instanceof List<?> fields) {
                    for (int j = 0; j < fields.size() - 1; j += 2) {
                        String fieldName = String.valueOf(fields.get(j));
                        String fieldValue = String.valueOf(fields.get(j + 1));
                        doc.put(fieldName, fieldValue);
                    }
                }

                documents.add(doc);
            }

            return new SearchResult(totalResults, documents);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse FT.SEARCH JSON response: {}", json, e);
            return new SearchResult(0, Collections.emptyList());
        }
    }

    /**
     * Parses the cjson-encoded FT.AGGREGATE response into an {@link AggregationResult}.
     *
     * <p>The JSON array format is:
     * {@code [totalResults, [field1, value1, field2, value2, ...], [field1, value1, ...], ...]}</p>
     *
     * @param json the cjson-encoded response string
     * @return the parsed aggregation result
     */
    private AggregationResult parseAggregationResult(String json) {
        if (json == null || json.isEmpty()) {
            return new AggregationResult(Collections.emptyList());
        }

        try {
            List<?> rawArray = objectMapper.readValue(json, List.class);
            if (rawArray == null || rawArray.size() <= 1) {
                return new AggregationResult(Collections.emptyList());
            }

            List<Map<String, String>> rows = new ArrayList<>();

            // FT.AGGREGATE returns: [totalResults, [f1, v1, f2, v2, ...], [f1, v1, ...], ...]
            for (int i = 1; i < rawArray.size(); i++) {
                Object rowObj = rawArray.get(i);
                if (rowObj instanceof List<?> fields) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int j = 0; j < fields.size() - 1; j += 2) {
                        String fieldName = String.valueOf(fields.get(j));
                        String fieldValue = String.valueOf(fields.get(j + 1));
                        row.put(fieldName, fieldValue);
                    }
                    rows.add(row);
                }
            }

            return new AggregationResult(rows);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse FT.AGGREGATE JSON response: {}", json, e);
            return new AggregationResult(Collections.emptyList());
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
