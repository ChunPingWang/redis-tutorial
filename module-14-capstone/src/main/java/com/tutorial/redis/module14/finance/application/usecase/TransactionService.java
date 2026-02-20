package com.tutorial.redis.module14.finance.application.usecase;

import com.tutorial.redis.module14.finance.domain.model.Transaction;
import com.tutorial.redis.module14.finance.domain.port.inbound.TransactionUseCase;
import com.tutorial.redis.module14.finance.domain.port.outbound.TransactionRankingPort;
import com.tutorial.redis.module14.finance.domain.port.outbound.TransactionSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing transaction use cases.
 *
 * <p>Coordinates between the {@link TransactionRankingPort} (sorted set
 * leaderboard) and the {@link TransactionSearchPort} (RediSearch index)
 * to record, rank, and search financial transactions.</p>
 */
@Service
public class TransactionService implements TransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRankingPort rankingPort;
    private final TransactionSearchPort searchPort;

    public TransactionService(TransactionRankingPort rankingPort,
                              TransactionSearchPort searchPort) {
        this.rankingPort = rankingPort;
        this.searchPort = searchPort;
    }

    @Override
    public void recordTransaction(Transaction tx) {
        log.info("Recording transaction {} (amount={})", tx.getTransactionId(), tx.getAmount());
        rankingPort.addToLeaderboard(tx.getTransactionId(), tx.getAmount());
        searchPort.indexTransaction(tx);
    }

    @Override
    public List<String> getTopTransactions(int count) {
        log.info("Retrieving top {} transactions", count);
        return rankingPort.getTopN(count);
    }

    @Override
    public List<String> searchTransactions(String query) {
        log.info("Searching transactions with query: {}", query);
        return searchPort.search(query);
    }
}
