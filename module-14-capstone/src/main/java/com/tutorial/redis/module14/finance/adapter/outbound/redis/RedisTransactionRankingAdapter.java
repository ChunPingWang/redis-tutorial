package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.module14.finance.domain.port.outbound.TransactionRankingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Redis adapter for transaction ranking operations.
 *
 * <p>Implements {@link TransactionRankingPort} using a Redis sorted set
 * as a leaderboard. Transactions are scored by their amount, allowing
 * efficient retrieval of the highest-value transactions.</p>
 */
@Component
public class RedisTransactionRankingAdapter implements TransactionRankingPort {

    private static final Logger log = LoggerFactory.getLogger(RedisTransactionRankingAdapter.class);

    private static final String LEADERBOARD_KEY = "finance:tx:leaderboard";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTransactionRankingAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void addToLeaderboard(String txId, double amount) {
        stringRedisTemplate.opsForZSet().add(LEADERBOARD_KEY, txId, amount);
        log.debug("Added transaction {} to leaderboard with amount {}", txId, amount);
    }

    @Override
    public List<String> getTopN(int n) {
        Set<String> result = stringRedisTemplate.opsForZSet()
                .reverseRange(LEADERBOARD_KEY, 0, n - 1);
        log.debug("Retrieved top {} transactions: {}", n, result);
        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }
}
