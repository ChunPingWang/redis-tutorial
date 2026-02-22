package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.model.RankEntry;
import com.tutorial.redis.module02.domain.port.outbound.TransactionRankingPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 排行榜管理 Use Case 單元測試
 * 驗證 ManageRankingService 正確委派操作至 TransactionRankingPort（Redis Sorted Set）。
 * 層級：Application（Use Case 業務邏輯）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManageRankingService 單元測試")
class ManageRankingServiceTest {

    @Mock
    private TransactionRankingPort transactionRankingPort;

    @InjectMocks
    private ManageRankingService service;

    // 驗證提交分數時正確委派至 Port 的 addOrUpdate
    @Test
    @DisplayName("submitScore_DelegatesToPort — 委派至 Port 的 addOrUpdate 方法")
    void submitScore_DelegatesToPort() {
        service.submitScore("leaderboard", "user-001", 100.0);

        verify(transactionRankingPort).addOrUpdate("leaderboard", "user-001", 100.0);
    }

    // 驗證取得排行榜時正確委派至 Port 的 getTopN，並回傳排序結果
    @Test
    @DisplayName("getLeaderboard_DelegatesToPort — 委派至 Port 的 getTopN 方法")
    void getLeaderboard_DelegatesToPort() {
        List<RankEntry> expected = List.of(
                new RankEntry("alice", 500.0, 0),
                new RankEntry("bob", 400.0, 1)
        );
        when(transactionRankingPort.getTopN("leaderboard", 2)).thenReturn(expected);

        List<RankEntry> result = service.getLeaderboard("leaderboard", 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMemberId()).isEqualTo("alice");
        verify(transactionRankingPort).getTopN("leaderboard", 2);
    }

    // 驗證組合 ZREVRANK 與 ZSCORE 結果，回傳含排名與分數的 RankEntry
    @Test
    @DisplayName("getMemberRank_ReturnsRankEntry — 組合 reverseRank 與 score 回傳 RankEntry")
    void getMemberRank_ReturnsRankEntry() {
        when(transactionRankingPort.getReverseRank("leaderboard", "alice"))
                .thenReturn(Optional.of(0L));
        when(transactionRankingPort.getScore("leaderboard", "alice"))
                .thenReturn(Optional.of(500.0));

        Optional<RankEntry> result = service.getMemberRank("leaderboard", "alice");

        assertThat(result).isPresent();
        assertThat(result.get().getMemberId()).isEqualTo("alice");
        assertThat(result.get().getScore()).isEqualTo(500.0);
        assertThat(result.get().getRank()).isEqualTo(1); // 0-based reverseRank + 1 = display rank
    }
}
