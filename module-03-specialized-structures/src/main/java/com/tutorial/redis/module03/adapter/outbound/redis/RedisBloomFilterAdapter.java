package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.module03.domain.port.outbound.BloomFilterPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis adapter for Bloom filter operations using Redis module commands.
 *
 * <p>Uses Lua scripts to invoke Bloom filter module commands
 * (BF.RESERVE, BF.ADD, BF.EXISTS, BF.MADD, BF.MEXISTS) since
 * {@code connection.execute()} is not reliably supported for module
 * commands across all Spring Data Redis / Lettuce versions.</p>
 *
 * <p>Key pattern: {@code filter:bloom:{filterName}}</p>
 */
@Component
public class RedisBloomFilterAdapter implements BloomFilterPort {

    private static final String KEY_PREFIX = "filter:bloom";

    private static final DefaultRedisScript<Long> BF_RESERVE =
            new DefaultRedisScript<>("redis.call('BF.RESERVE', KEYS[1], ARGV[1], ARGV[2]); return 1", Long.class);

    private static final DefaultRedisScript<Long> BF_ADD =
            new DefaultRedisScript<>("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

    private static final DefaultRedisScript<Long> BF_EXISTS =
            new DefaultRedisScript<>("return redis.call('BF.EXISTS', KEYS[1], ARGV[1])", Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisBloomFilterAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void createFilter(String filterName, double errorRate, long capacity) {
        String key = buildKey(filterName);
        redisTemplate.execute(BF_RESERVE, List.of(key),
                String.valueOf(errorRate), String.valueOf(capacity));
    }

    @Override
    public boolean add(String filterName, String item) {
        String key = buildKey(filterName);
        Long result = redisTemplate.execute(BF_ADD, List.of(key), item);
        return result != null && result == 1L;
    }

    @Override
    public boolean mightContain(String filterName, String item) {
        String key = buildKey(filterName);
        Long result = redisTemplate.execute(BF_EXISTS, List.of(key), item);
        return result != null && result == 1L;
    }

    @Override
    public List<Boolean> addAll(String filterName, List<String> items) {
        String key = buildKey(filterName);
        String luaScript = buildMultiItemScript("BF.MADD", items.size());
        DefaultRedisScript<List> script = new DefaultRedisScript<>(luaScript, List.class);
        @SuppressWarnings("unchecked")
        List<Long> result = redisTemplate.execute(script, List.of(key), items.toArray(new String[0]));
        return toBooleanList(result, items.size());
    }

    @Override
    public List<Boolean> mightContainAll(String filterName, List<String> items) {
        String key = buildKey(filterName);
        String luaScript = buildMultiItemScript("BF.MEXISTS", items.size());
        DefaultRedisScript<List> script = new DefaultRedisScript<>(luaScript, List.class);
        @SuppressWarnings("unchecked")
        List<Long> result = redisTemplate.execute(script, List.of(key), items.toArray(new String[0]));
        return toBooleanList(result, items.size());
    }

    private String buildMultiItemScript(String command, int argCount) {
        StringBuilder sb = new StringBuilder("return redis.call('").append(command).append("', KEYS[1]");
        for (int i = 1; i <= argCount; i++) {
            sb.append(", ARGV[").append(i).append("]");
        }
        sb.append(")");
        return sb.toString();
    }

    private List<Boolean> toBooleanList(List<Long> result, int expectedSize) {
        if (result == null || result.isEmpty()) {
            List<Boolean> falseList = new ArrayList<>(expectedSize);
            for (int i = 0; i < expectedSize; i++) {
                falseList.add(false);
            }
            return falseList;
        }
        return result.stream()
                .map(val -> val != null && val == 1L)
                .toList();
    }

    private String buildKey(String filterName) {
        return KEY_PREFIX + ":" + filterName;
    }
}
