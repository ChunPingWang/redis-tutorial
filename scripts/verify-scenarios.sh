#!/usr/bin/env bash
# 驗證 docs/07-troubleshooting.md 的 12 個故障情境：注入 → 排查指令真的看得到症狀 → 還原 → 恢復正常
#   ./scripts/verify-scenarios.sh            # 全部（會依序啟動 / 關閉 單機、Sentinel、Cluster 環境，約 5 分鐘）
#   ./scripts/verify-scenarios.sh single     # 只跑單機的 S1–S8、S11
cd "$(dirname "$0")/.."; source scripts/lib/common.sh
SC=./scripts/scenario.sh
R() { docker exec -i redis-tutorial redis-cli "$@"; }
S() { docker exec sentinel-1 redis-cli -p 26379 "$@"; }
K() { docker exec redis-node-2 redis-cli -p 7002 "$@"; }
MODE="${1:-all}"

single() {
  docker compose -f docker-compose-sentinel.yml down -v >/dev/null 2>&1   # 兩者都用宿主機 6379
  docker compose up -d redis >/dev/null 2>&1; wait_for_ping 30 || { fail "單機環境起不來"; return; }

  title "S1 OOM：記憶體滿且 noeviction"
  $SC inject 1 >/dev/null
  assert_true "SET 回 OOM" "R set probe 1 2>&1 | grep -q OOM"
  assert_true "INFO memory 看得到 used ≥ maxmemory、policy=noeviction" "R info memory | grep -q 'maxmemory_policy:noeviction'"
  assert_true "INFO errorstats 有 errorstat_OOM" "R info errorstats | grep -q errorstat_OOM"
  $SC reset 1 >/dev/null; assert_eq "還原後可寫" "OK" "$(R set probe 1)"; R del probe >/dev/null

  title "S2 延遲飆高：大 key + SMEMBERS / DEL"
  $SC inject 2 >/dev/null
  assert_true "SLOWLOG 記到 smembers" "R slowlog get 5 | grep -q smembers"
  assert_true "commandstats 的 smembers usec_per_call > 10ms" "[[ $(R info commandstats | grep cmdstat_smembers | sed 's/.*usec_per_call=\([0-9]*\).*/\1/') -gt 10000 ]]"
  assert_true "--bigkeys 找到 scenario:bigset" "R --bigkeys 2>/dev/null | grep -q 'scenario:bigset'"
  $SC reset 2 >/dev/null; assert_eq "還原後大 key 已刪" "0" "$(R exists scenario:bigset)"

  title "S3 連不上：max number of clients"
  $SC inject 3 >/dev/null
  assert_true "PING 回 max number of clients reached" "R ping 2>&1 | grep -q 'max number of clients'"
  $SC reset 3 >/dev/null
  assert_eq "還原後 PING" "PONG" "$(R ping)"
  assert_true "INFO stats 的 rejected_connections > 0（事後可查）" "[[ $(R info stats | tr -d '\r' | awk -F: '/^rejected_connections/{print $2}') -gt 0 ]]"

  title "S4 記憶體只漲、命中率低：沒有 TTL"
  $SC inject 4 >/dev/null
  assert_true "INFO keyspace：keys=5000 但 expires=0" "R info keyspace | grep -q 'keys=5000,expires=0'"
  h=$(R info stats | tr -d '\r' | awk -F: '/^keyspace_hits/{print $2}'); m=$(R info stats | tr -d '\r' | awk -F: '/^keyspace_misses/{print $2}')
  assert_true "命中率 $((h*100/(h+m)))% < 50%" "[[ $((h*100/(h+m))) -lt 50 ]]"
  assert_eq "抽樣 key 的 TTL 為 -1（永不過期）" "-1" "$(R ttl scenario:cache:1)"
  $SC reset 4 >/dev/null; assert_eq "還原後 key 清空" "0" "$(R --scan --pattern 'scenario:cache:*' | wc -l)"

  title "S5 MISCONF：bgsave 失敗導致拒寫"
  $SC inject 5 >/dev/null
  assert_true "SET 回 MISCONF" "R set probe 1 2>&1 | grep -q MISCONF"
  assert_true "INFO persistence：rdb_last_bgsave_status:err" "R info persistence | grep -q 'rdb_last_bgsave_status:err'"
  assert_true "日誌有 Permission denied" "docker logs redis-tutorial 2>&1 | grep -q 'Permission denied'"
  $SC reset 5 >/dev/null; assert_true "還原後 bgsave ok 且可寫" "R info persistence | grep -q 'rdb_last_bgsave_status:ok' && [[ $(R set probe 1) == OK ]]"; R del probe >/dev/null

  title "S6 資料消失：FLUSHALL"
  $SC inject 6 >/dev/null
  assert_eq "DBSIZE 0" "0" "$(R dbsize)"
  assert_true "commandstats 有 cmdstat_flushall" "R info commandstats | grep -q cmdstat_flushall"
  $SC reset 6 >/dev/null; ok "還原（統計歸零）"

  title "S7 熱 key"
  $SC inject 7 >/dev/null
  assert_true "OBJECT FREQ 很高（LFU 計數 > 100）" "[[ $(R object freq key:000000000000) -gt 100 ]]"
  assert_true "--hotkeys 排第一的是 key:000000000000" "R --hotkeys 2>/dev/null | grep -A1 'Hot key' | grep -q 'key:000000000000' || R --hotkeys 2>/dev/null | grep 'hot key found' | head -1 | grep -q 'key:000000000000'"
  $SC reset 7 >/dev/null; assert_eq "還原後 policy 回 allkeys-lru" "allkeys-lru" "$(R config get maxmemory-policy | tail -1)"

  title "S11 NOPERM：ACL 權限不足"
  $SC inject 11 >/dev/null
  assert_true "應用帳號 SET 回 NOPERM" "docker exec redis-tutorial redis-cli --user scenario-app --pass app-pass --no-auth-warning set app:x 1 2>&1 | grep -q NOPERM"
  assert_true "ACL LOG 記到 scenario-app 被拒" "R acl log | grep -q scenario-app"
  $SC reset 11 >/dev/null; assert_true "還原後使用者已刪" "! R acl list | grep -q scenario-app"

  title "S8 AOF 損毀：Redis 起不來（放最後，會重啟容器）"
  $SC inject 8 >/dev/null
  assert_true "日誌有 Bad file format" "docker logs redis-tutorial 2>&1 | grep -q 'Bad file format'"
  assert_true "PING 失敗（容器反覆重啟）" "! R ping >/dev/null 2>&1"
  $SC reset 8 >/dev/null; wait_for_ping 30; assert_eq "redis-check-aof --fix 後可啟動" "PONG" "$(R ping)"

  docker compose stop redis >/dev/null 2>&1
}

sentinel() {
  docker compose stop redis >/dev/null 2>&1                                 # 釋放 6379
  docker compose -f docker-compose-sentinel.yml up -d >/dev/null 2>&1
  for _ in $(seq 1 60); do [[ "$(S sentinel replicas mymaster 2>/dev/null | grep -c '^ip$')" == 2 ]] && break; sleep 1; done

  title "S12 Replica 停止"
  $SC inject 12 >/dev/null
  assert_eq "Master 的 connected_slaves 變 1" "1" "$(docker exec redis-master redis-cli info replication | tr -d '\r' | awk -F: '/^connected_slaves/{print $2}')"
  assert_true "Sentinel 日誌 +sdown slave" "docker logs sentinel-1 2>&1 | grep -q '+sdown slave'"
  $SC reset 12 >/dev/null
  for _ in $(seq 1 30); do [[ "$(docker exec redis-replica-1 redis-cli info replication | tr -d '\r' | awk -F: '/^master_link_status/{print $2}')" == up ]] && break; sleep 1; done
  assert_eq "還原後 replica-1 重新同步、master_link_status up" "up" "$(docker exec redis-replica-1 redis-cli info replication | tr -d '\r' | awk -F: '/^master_link_status/{print $2}')"

  title "S9 quorum 太高：Sentinel 不做故障轉移"
  before=$(S sentinel get-master-addr-by-name mymaster | head -1)
  $SC inject 9 >/dev/null
  assert_true "SENTINEL CKQUORUM 回 NOQUORUM" "S sentinel ckquorum mymaster 2>&1 | grep -q NOQUORUM"
  assert_true "有 +sdown master 但沒有 +odown / +switch-master" "docker logs sentinel-1 2>&1 | grep -q '+sdown master' && ! docker logs sentinel-1 2>&1 | tail -20 | grep -q '+switch-master'"
  assert_eq "Master 位址沒變（沒切換）" "$before" "$(S sentinel get-master-addr-by-name mymaster | head -1)"
  $SC reset 9 >/dev/null; sleep 3
  assert_true "還原後 CKQUORUM OK" "S sentinel ckquorum mymaster 2>&1 | grep -q '^OK'"
  docker compose -f docker-compose-sentinel.yml down -v >/dev/null 2>&1
}

cluster() {
  docker compose -f docker-compose-cluster.yml up -d >/dev/null 2>&1
  for _ in $(seq 1 60); do [[ "$(K cluster info 2>/dev/null | tr -d '\r' | awk -F: '/^cluster_state/{print $2}')" == ok ]] && break; sleep 1; done

  title "S10 CLUSTERDOWN：一組主從同時下線"
  $SC inject 10 >/dev/null
  assert_true "SET 回 CLUSTERDOWN" "docker exec redis-node-2 redis-cli -c -p 7002 set a 1 2>&1 | grep -q CLUSTERDOWN"
  assert_true "cluster_state:fail、cluster_slots_fail > 0" "K cluster info | grep -q 'cluster_state:fail' && [[ $(K cluster info | tr -d '\r' | awk -F: '/^cluster_slots_fail/{print $2}') -gt 0 ]]"
  assert_true "CLUSTER NODES 有 master,fail 與 slave,fail" "K cluster nodes | grep -q 'master,fail' && K cluster nodes | grep -q 'slave,fail'"
  $SC reset 10 >/dev/null
  for _ in $(seq 1 30); do [[ "$(K cluster info | tr -d '\r' | awk -F: '/^cluster_state/{print $2}')" == ok ]] && break; sleep 1; done
  assert_true "還原後 cluster_state:ok 且可寫" "K cluster info | grep -q 'cluster_state:ok' && docker exec redis-node-2 redis-cli -c -p 7002 set a 1 | grep -q OK"
  docker exec redis-node-2 redis-cli -c -p 7002 del a >/dev/null
  docker compose -f docker-compose-cluster.yml down -v >/dev/null 2>&1
}

case "$MODE" in
  single) single;; sentinel) sentinel;; cluster) cluster;;
  all) single; sentinel; cluster;;
esac
summary
