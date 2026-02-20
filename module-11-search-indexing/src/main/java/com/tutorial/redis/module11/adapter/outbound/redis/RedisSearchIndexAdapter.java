package com.tutorial.redis.module11.adapter.outbound.redis;

import com.tutorial.redis.module11.domain.port.outbound.SearchIndexPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis adapter for RediSearch index management using Lua scripts.
 *
 * <p>Invokes FT.CREATE, FT.DROPINDEX, and FT.INFO via {@link DefaultRedisScript}
 * because {@code connection.execute()} is not supported for module commands
 * in Lettuce / Spring Data Redis 4.x.</p>
 */
@Component
public class RedisSearchIndexAdapter implements SearchIndexPort {

    private static final Logger log = LoggerFactory.getLogger(RedisSearchIndexAdapter.class);

    private static final DefaultRedisScript<String> FT_DROPINDEX =
            new DefaultRedisScript<>("return redis.call('FT.DROPINDEX', KEYS[1])", String.class);

    private static final DefaultRedisScript<String> FT_INDEX_EXISTS = new DefaultRedisScript<>(
            "local ok, err = pcall(redis.call, 'FT.INFO', KEYS[1]) " +
                    "if ok then return '1' else return '0' end",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisSearchIndexAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void createIndex(String indexName, String prefix, Map<String, String> schema) {
        List<String> args = new ArrayList<>();
        args.add(prefix);
        args.add("SCHEMA");
        for (Map.Entry<String, String> entry : schema.entrySet()) {
            args.add(entry.getKey());
            String[] typeParts = entry.getValue().split("\\s+");
            for (String part : typeParts) {
                args.add(part);
            }
        }

        String luaScript = buildCreateIndexScript(args.size());
        DefaultRedisScript<String> script = new DefaultRedisScript<>(luaScript, String.class);
        stringRedisTemplate.execute(script, List.of(indexName), (Object[]) args.toArray(new String[0]));
        log.info("Created RediSearch index '{}' with prefix '{}' and {} schema fields",
                indexName, prefix, schema.size());
    }

    @Override
    public void dropIndex(String indexName) {
        stringRedisTemplate.execute(FT_DROPINDEX, List.of(indexName));
        log.info("Dropped RediSearch index '{}'", indexName);
    }

    @Override
    public boolean indexExists(String indexName) {
        String result = stringRedisTemplate.execute(FT_INDEX_EXISTS, List.of(indexName));
        return "1".equals(result);
    }

    /**
     * Builds a Lua script for FT.CREATE that dynamically unpacks ARGV.
     *
     * <p>The generated script constructs the full FT.CREATE command:
     * {@code FT.CREATE KEYS[1] ON HASH PREFIX 1 ARGV[1] ARGV[2] ... ARGV[N]}</p>
     *
     * @param argCount the total number of ARGV parameters
     * @return the Lua script text
     */
    private String buildCreateIndexScript(int argCount) {
        StringBuilder lua = new StringBuilder();
        lua.append("local args = {'FT.CREATE', KEYS[1], 'ON', 'HASH', 'PREFIX', '1', ARGV[1]} ");
        lua.append("for i = 2, #ARGV do ");
        lua.append("args[#args + 1] = ARGV[i] ");
        lua.append("end ");
        lua.append("return redis.call(unpack(args))");
        return lua.toString();
    }
}
