package com.tutorial.redis.module09.domain.service;

import com.tutorial.redis.module09.domain.model.FailoverEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure domain service that generates the educational failover event sequence.
 *
 * <p>This service has zero framework dependencies — it operates entirely
 * on domain knowledge about the Redis Sentinel failover process. Each
 * event describes one step in the automated failover sequence that
 * Sentinel performs when a master becomes unavailable.</p>
 *
 * <p>The failover steps are:</p>
 * <ol>
 *   <li><strong>SDOWN</strong> — a single Sentinel detects the master is unresponsive</li>
 *   <li><strong>ODOWN</strong> — a quorum of Sentinels confirms the master is down</li>
 *   <li><strong>FAILOVER_START</strong> — the elected Sentinel leader begins failover</li>
 *   <li><strong>NEW_MASTER</strong> — a replica is promoted to master</li>
 *   <li><strong>FAILOVER_END</strong> — the old master rejoins as a replica</li>
 * </ol>
 */
public class FailoverProcessService {

    /**
     * Returns the ordered sequence of failover events describing how
     * Redis Sentinel performs automatic failover.
     *
     * <p>Timestamps are generated relative to the current system time,
     * with each step offset by 1 second for illustrative purposes.</p>
     *
     * @return a list of {@link FailoverEvent} in chronological order
     */
    public List<FailoverEvent> describeFailoverProcess() {
        List<FailoverEvent> events = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        events.add(new FailoverEvent(
                "SDOWN",
                "Sentinel 偵測到 Master 無回應，標記為主觀下線 (Subjectively Down)",
                baseTime
        ));

        events.add(new FailoverEvent(
                "ODOWN",
                "多數 Sentinel 確認 Master 下線，標記為客觀下線 (Objectively Down)",
                baseTime + 1000
        ));

        events.add(new FailoverEvent(
                "FAILOVER_START",
                "Sentinel Leader 開始執行故障轉移",
                baseTime + 2000
        ));

        events.add(new FailoverEvent(
                "NEW_MASTER",
                "選出新 Master (基於 replica-priority、replication offset、run ID)",
                baseTime + 3000
        ));

        events.add(new FailoverEvent(
                "FAILOVER_END",
                "故障轉移完成，舊 Master 重新加入成為 Replica",
                baseTime + 4000
        ));

        return events;
    }
}
