package com.tutorial.redis.module02.adapter.inbound.rest;

import com.tutorial.redis.module02.application.dto.RankEntryResponse;
import com.tutorial.redis.module02.domain.port.inbound.ManageRankingUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for ranking/leaderboard management.
 *
 * <p>Demonstrates Redis Sorted Set operations through leaderboard endpoints.
 * Supports score submission, top-N queries, and individual rank lookups.</p>
 */
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final ManageRankingUseCase manageRankingUseCase;

    public RankingController(ManageRankingUseCase manageRankingUseCase) {
        this.manageRankingUseCase = manageRankingUseCase;
    }

    @PostMapping("/{rankingKey}/entries")
    public ResponseEntity<Void> submitScore(
            @PathVariable String rankingKey,
            @RequestBody ScoreSubmission submission) {
        manageRankingUseCase.submitScore(rankingKey, submission.memberId(), submission.score());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{rankingKey}/top")
    public ResponseEntity<List<RankEntryResponse>> getLeaderboard(
            @PathVariable String rankingKey,
            @RequestParam(defaultValue = "10") int n) {
        List<RankEntryResponse> leaderboard = manageRankingUseCase.getLeaderboard(rankingKey, n)
                .stream()
                .map(RankEntryResponse::from)
                .toList();
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/{rankingKey}/members/{memberId}/rank")
    public ResponseEntity<RankEntryResponse> getMemberRank(
            @PathVariable String rankingKey,
            @PathVariable String memberId) {
        return manageRankingUseCase.getMemberRank(rankingKey, memberId)
                .map(RankEntryResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Request body for score submission.
     */
    record ScoreSubmission(String memberId, double score) {
    }
}
