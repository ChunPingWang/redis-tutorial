package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module02.domain.model.RankEntry;
import com.tutorial.redis.module02.domain.port.outbound.TransactionRankingPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 交易排行榜 Adapter 整合測試
 * 驗證使用 Redis Sorted Set（ZADD/ZSCORE/ZREVRANGE/ZREVRANK/ZCOUNT/ZREM）實作排行榜。
 * 層級：Adapter（外部端口實作）
 */
@DisplayName("RedisTransactionRankingAdapter 整合測試")
class RedisTransactionRankingAdapterTest extends AbstractRedisIntegrationTest {

    private static final String RANKING_KEY = "test-leaderboard";

    @Autowired
    private TransactionRankingPort rankingPort;

    // 驗證 ZADD 新增成員至 Sorted Set，並能透過 ZSCORE 查詢分數
    @Test
    @DisplayName("addOrUpdate_WhenNewMember_AddsToRanking — 新增成員至排行榜")
    void addOrUpdate_WhenNewMember_AddsToRanking() {
        rankingPort.addOrUpdate(RANKING_KEY, "user-001", 100.0);

        Optional<Double> score = rankingPort.getScore(RANKING_KEY, "user-001");
        assertThat(score).isPresent();
        assertThat(score.get()).isEqualTo(100.0);
        assertThat(rankingPort.count(RANKING_KEY)).isEqualTo(1);
    }

    // 驗證 ZADD 對已存在的成員會更新其分數，不會重複新增
    @Test
    @DisplayName("addOrUpdate_WhenExistingMember_UpdatesScore — 更新已存在成員的分數")
    void addOrUpdate_WhenExistingMember_UpdatesScore() {
        rankingPort.addOrUpdate(RANKING_KEY, "user-002", 100.0);
        rankingPort.addOrUpdate(RANKING_KEY, "user-002", 200.0);

        Optional<Double> score = rankingPort.getScore(RANKING_KEY, "user-002");
        assertThat(score).isPresent();
        assertThat(score.get()).isEqualTo(200.0);
        assertThat(rankingPort.count(RANKING_KEY)).isEqualTo(1);
    }

    // 驗證 ZINCRBY 原子增量分數操作，回傳更新後的分數
    @Test
    @DisplayName("incrementScore_WhenCalled_ReturnsNewScore — 增量分數並回傳新值")
    void incrementScore_WhenCalled_ReturnsNewScore() {
        rankingPort.addOrUpdate(RANKING_KEY, "user-003", 50.0);

        double newScore = rankingPort.incrementScore(RANKING_KEY, "user-003", 25.0);

        assertThat(newScore).isEqualTo(75.0);
    }

    // 驗證 ZREVRANGE 取得分數由高到低的前 N 名排行
    @Test
    @DisplayName("getTopN_WhenMultipleMembers_ReturnsSortedByScoreDesc — 取得 Top N 排名（降冪）")
    void getTopN_WhenMultipleMembers_ReturnsSortedByScoreDesc() {
        rankingPort.addOrUpdate(RANKING_KEY, "alice", 300.0);
        rankingPort.addOrUpdate(RANKING_KEY, "bob", 500.0);
        rankingPort.addOrUpdate(RANKING_KEY, "charlie", 100.0);
        rankingPort.addOrUpdate(RANKING_KEY, "diana", 400.0);
        rankingPort.addOrUpdate(RANKING_KEY, "eve", 200.0);

        List<RankEntry> top3 = rankingPort.getTopN(RANKING_KEY, 3);

        assertThat(top3).hasSize(3);
        assertThat(top3.get(0).getMemberId()).isEqualTo("bob");
        assertThat(top3.get(0).getScore()).isEqualTo(500.0);
        assertThat(top3.get(1).getMemberId()).isEqualTo("diana");
        assertThat(top3.get(1).getScore()).isEqualTo(400.0);
        assertThat(top3.get(2).getMemberId()).isEqualTo("alice");
        assertThat(top3.get(2).getScore()).isEqualTo(300.0);
    }

    // 驗證 ZREVRANK 回傳以 0 為基底的反向排名（最高分為 0）
    @Test
    @DisplayName("getReverseRank_ReturnsZeroBasedRank — 最高分 rank 為 0")
    void getReverseRank_ReturnsZeroBasedRank() {
        rankingPort.addOrUpdate(RANKING_KEY, "alice", 300.0);
        rankingPort.addOrUpdate(RANKING_KEY, "bob", 500.0);
        rankingPort.addOrUpdate(RANKING_KEY, "charlie", 100.0);

        Optional<Long> rank = rankingPort.getReverseRank(RANKING_KEY, "bob");

        assertThat(rank).isPresent();
        assertThat(rank.get()).isEqualTo(0);
    }

    // 驗證 ZSCORE 查詢指定成員的分數
    @Test
    @DisplayName("getScore_WhenMemberExists_ReturnsScore — 查詢成員分數")
    void getScore_WhenMemberExists_ReturnsScore() {
        rankingPort.addOrUpdate(RANKING_KEY, "user-score", 123.45);

        Optional<Double> score = rankingPort.getScore(RANKING_KEY, "user-score");

        assertThat(score).isPresent();
        assertThat(score.get()).isEqualTo(123.45);
    }

    // 驗證 ZREM 移除成員後，排行榜中不再包含該成員
    @Test
    @DisplayName("remove_WhenMemberExists_RemovesFromRanking — 移除成員")
    void remove_WhenMemberExists_RemovesFromRanking() {
        rankingPort.addOrUpdate(RANKING_KEY, "user-remove", 100.0);
        assertThat(rankingPort.count(RANKING_KEY)).isEqualTo(1);

        rankingPort.remove(RANKING_KEY, "user-remove");

        assertThat(rankingPort.getScore(RANKING_KEY, "user-remove")).isEmpty();
        assertThat(rankingPort.count(RANKING_KEY)).isEqualTo(0);
    }

    // 驗證 ZCOUNT 統計指定分數範圍內的成員數量
    @Test
    @DisplayName("countByScoreRange_WhenInRange_ReturnsCount — 範圍分數計數")
    void countByScoreRange_WhenInRange_ReturnsCount() {
        rankingPort.addOrUpdate(RANKING_KEY, "a", 100.0);
        rankingPort.addOrUpdate(RANKING_KEY, "b", 200.0);
        rankingPort.addOrUpdate(RANKING_KEY, "c", 300.0);
        rankingPort.addOrUpdate(RANKING_KEY, "d", 400.0);
        rankingPort.addOrUpdate(RANKING_KEY, "e", 500.0);

        long count = rankingPort.countByScoreRange(RANKING_KEY, 200.0, 400.0);

        assertThat(count).isEqualTo(3);
    }

    // 驗證 Redis key 遵循 ranking:{rankingKey} 命名規範
    @Test
    @DisplayName("key_FollowsNamingConvention — 驗證 key 符合 ranking:* 命名規範")
    void key_FollowsNamingConvention() {
        rankingPort.addOrUpdate("my-rank", "user-001", 50.0);

        Set<String> keys = stringRedisTemplate.keys("ranking:my-rank");
        assertThat(keys).isNotNull().hasSize(1);
        assertThat(keys.iterator().next()).isEqualTo("ranking:my-rank");
    }
}
