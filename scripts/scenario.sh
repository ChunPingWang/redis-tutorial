#!/usr/bin/env bash
# 故障情境模擬：把 docs/07-troubleshooting.md 的每個情境「真的弄壞」，讓你練習排查；練完一鍵還原
#
#   ./scripts/scenario.sh list                 # 列出情境
#   ./scripts/scenario.sh inject <編號>         # 注入故障
#   ./scripts/scenario.sh reset  <編號>         # 還原
#
# 需要的環境：S1–S8 用單機（docker compose up -d redis）；S9、S12 用 Sentinel；S10 用 Cluster。
cd "$(dirname "$0")/.."; source scripts/lib/common.sh
C="docker compose"; CS="docker compose -f docker-compose-sentinel.yml"; CC="docker compose -f docker-compose-cluster.yml"
R() { docker exec -i redis-tutorial redis-cli "$@"; }                 # 單機
need() { docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null | grep -q true || { echo "需要先啟動 $2"; exit 1; }; }

declare -A DESC=(
  [1]="寫入被拒 OOM：記憶體滿且 noeviction"
  [2]="延遲飆高：大 key + O(N) 指令（SMEMBERS / DEL 百萬元素）"
  [3]="連不上：max number of clients reached"
  [4]="記憶體只漲不跌、命中率低：快取沒有 TTL"
  [5]="MISCONF：bgsave 失敗導致拒寫（資料目錄不可寫）"
  [6]="資料突然全部消失：有人跑了 FLUSHALL"
  [7]="CPU 單核打滿：熱 key"
  [8]="Redis 起不來：AOF 檔損毀（Bad file format）"
  [9]="Sentinel 不做故障轉移：quorum 設得比 Sentinel 數量還多"
  [10]="Cluster 回 CLUSTERDOWN：一組主從同時下線、slot 沒人服務"
  [11]="應用一直收到 NOPERM：ACL 權限不足"
  [12]="Replica 斷線、複寫延遲：Replica 停止"
)

inject() { case "$1" in
  1) need redis-tutorial "docker compose up -d redis"
     R config set maxmemory 3mb >/dev/null; R config set maxmemory-policy noeviction >/dev/null
     for i in $(seq 1 400); do echo "SET scenario:fill:$i $(head -c 10000 /dev/zero | tr '\0' x)"; done | R --pipe >/dev/null 2>&1
     echo "已注入：maxmemory 3mb + noeviction，塞入 4 MB 資料。試試：redis-cli SET a 1";;
  2) need redis-tutorial "docker compose up -d redis"
     R config set slowlog-log-slower-than 10000 >/dev/null; R slowlog reset >/dev/null
     for i in $(seq 1 1000000); do echo "SADD scenario:bigset m$i"; done | R --pipe >/dev/null 2>&1
     R smembers scenario:bigset >/dev/null; R del scenario:bigset >/dev/null
     for i in $(seq 1 300000); do echo "SADD scenario:bigset m$i"; done | R --pipe >/dev/null 2>&1
     echo "已注入：建立 100 萬元素的 Set 並執行 SMEMBERS / DEL（留下 30 萬元素的大 key）。試試：redis-cli SLOWLOG GET 5";;
  3) need redis-tutorial "docker compose up -d redis"
     R config set maxclients 3 >/dev/null
     # 用三個獨立容器各占一條連線（之後可以從外面 docker rm 掉；Redis 本身已經連不進去）
     for i in 1 2 3; do docker run -d --rm --name scenario-hold-$i --network container:redis-tutorial redis:8-alpine redis-cli subscribe scenario:hold >/dev/null; done; sleep 2
     echo "已注入：maxclients 3，並有 3 個閒置連線占著。試試：redis-cli PING";;
  4) need redis-tutorial "docker compose up -d redis"
     R config resetstat >/dev/null
     for i in $(seq 1 5000); do echo "SET scenario:cache:$i v$i"; done | R --pipe >/dev/null 2>&1
     for i in $(seq 1 3000); do echo "GET scenario:cache:miss:$i"; done | R --pipe >/dev/null 2>&1
     for i in $(seq 1 1000); do echo "GET scenario:cache:$i"; done | R --pipe >/dev/null 2>&1
     echo "已注入：5000 個沒有 TTL 的快取 key，命中率 25%。試試：redis-cli INFO keyspace / INFO stats";;
  5) need redis-tutorial "docker compose up -d redis"
     docker exec -u root redis-tutorial chmod 555 /data; R bgsave >/dev/null; sleep 2
     echo "已注入：資料目錄不可寫，bgsave 失敗。試試：redis-cli SET a 1 / INFO persistence";;
  6) need redis-tutorial "docker compose up -d redis"
     for i in $(seq 1 2000); do echo "SET scenario:important:$i v"; done | R --pipe >/dev/null 2>&1
     R config resetstat >/dev/null; R flushall >/dev/null
     echo "已注入：有人執行了 FLUSHALL。試試：redis-cli DBSIZE / INFO commandstats";;
  7) need redis-tutorial "docker compose up -d redis"
     R config set maxmemory-policy allkeys-lfu >/dev/null
     for i in $(seq 1 2000); do echo "SET scenario:cold:$i v"; done | R --pipe >/dev/null 2>&1
     R set key:000000000000 hot-value >/dev/null    # redis-benchmark 對不存在的 key GET 不會建立它，要先放進去
     docker exec redis-tutorial redis-benchmark -q -t get -n 300000 -r 1 -c 20 >/dev/null 2>&1   # -r 1：只打一個 key：key:000000000000
     echo "已注入：單一 key 承受 30 萬次讀取。試試：redis-cli --hotkeys / INFO commandstats";;
  8) need redis-tutorial "docker compose up -d redis"
     # 先 rewrite 讓 incr 檔從零開始，之後寫入的 100 筆小指令才會落在檔案前段
     R bgrewriteaof >/dev/null; for _ in $(seq 1 30); do [[ "$(R info persistence | tr -d '\r' | awk -F: '/^aof_rewrite_in_progress/{print $2}')" == 0 ]] && break; sleep 1; done
     for i in $(seq 1 100); do echo "SET scenario:aof:$i v"; done | R --pipe >/dev/null 2>&1; sleep 1
     $C stop redis >/dev/null 2>&1
     # 覆寫協定框架（*3\r\n$3\r\nSET…）而不是 value 內容：尾端截斷會被 aof-load-truncated yes 自動容忍，
     # 中段的格式錯誤才會讓 Redis 拒絕啟動（Bad file format）
     docker run --rm -v redis-tutorial_redis-data:/data redis:8-alpine sh -c 'f=$(ls /data/appendonlydir/*.incr.aof | tail -1); printf "GARBAGE-CORRUPTION" | dd of="$f" bs=1 seek=60 conv=notrunc 2>/dev/null'
     $C start redis >/dev/null 2>&1; sleep 4
     echo "已注入：AOF 中段損毀，Redis 啟動失敗並反覆重啟。試試：docker logs redis-tutorial --tail 5";;
  9) need sentinel-1 "docker compose -f docker-compose-sentinel.yml up -d"
     for s in sentinel-1 sentinel-2 sentinel-3; do docker exec $s redis-cli -p 26379 sentinel set mymaster quorum 5 >/dev/null; done
     $CS stop redis-master >/dev/null 2>&1; sleep 8
     echo "已注入：quorum 5 但只有 3 個 Sentinel，Master 已停。試試：docker exec sentinel-1 redis-cli -p 26379 SENTINEL CKQUORUM mymaster";;
  10) need redis-node-1 "docker compose -f docker-compose-cluster.yml up -d"
     # 找 node-1 的 replica 一起停
     rid=$(docker exec redis-node-1 redis-cli -p 7001 cluster myid)
     rep=$(docker exec redis-node-1 redis-cli -p 7001 cluster nodes | awk -v id="$rid" '$4==id {print $2}' | cut -d: -f1)
     $CC stop redis-node-1 "redis-node-${rep##*.1}" >/dev/null 2>&1; sleep 8
     echo "已注入：redis-node-1 與它的 Replica 同時停止。試試：docker exec redis-node-2 redis-cli -c -p 7002 SET a 1";;
  11) need redis-tutorial "docker compose up -d redis"
     R acl setuser scenario-app on '>app-pass' '~app:*' '+@read' >/dev/null; R acl log reset >/dev/null
     docker exec redis-tutorial redis-cli --user scenario-app --pass app-pass --no-auth-warning set app:x 1 >/dev/null 2>&1
     docker exec redis-tutorial redis-cli --user scenario-app --pass app-pass --no-auth-warning get other:x >/dev/null 2>&1
     echo "已注入：應用帳號 scenario-app 只有 ~app:* 的讀取權限。試試：redis-cli ACL LOG";;
  12) need redis-replica-1 "docker compose -f docker-compose-sentinel.yml up -d"
     $CS stop redis-replica-1 >/dev/null 2>&1; sleep 8    # 等過 down-after-milliseconds（5 秒）讓 Sentinel 標記 +sdown
     echo "已注入：redis-replica-1 停止。試試：docker exec redis-master redis-cli INFO replication";;
  *) echo "未知情境 $1"; exit 1;;
esac; }

reset() { case "$1" in
  1) R config set maxmemory 256mb >/dev/null; R config set maxmemory-policy allkeys-lru >/dev/null
     R --scan --pattern 'scenario:fill:*' | sed 's/^/UNLINK /' | R >/dev/null;;
  2) R unlink scenario:bigset >/dev/null; R slowlog reset >/dev/null;;
  3) docker rm -f scenario-hold-1 scenario-hold-2 scenario-hold-3 >/dev/null 2>&1; sleep 1   # 連不進去，只能從外面殺掉占連線的行程
     R config set maxclients 10000 >/dev/null;;
  4) R --scan --pattern 'scenario:cache:*' | sed 's/^/UNLINK /' | R >/dev/null; R config resetstat >/dev/null;;
  5) docker exec -u root redis-tutorial chmod 755 /data; R bgsave >/dev/null; sleep 2;;
  6) R config resetstat >/dev/null;;
  7) R config set maxmemory-policy allkeys-lru >/dev/null; R del key:000000000000 >/dev/null
     R --scan --pattern 'scenario:cold:*' | sed 's/^/UNLINK /' | R >/dev/null;;
  8) $C stop redis >/dev/null 2>&1
     docker run --rm -v redis-tutorial_redis-data:/data redis:8-alpine sh -c 'f=$(ls /data/appendonlydir/*.incr.aof | tail -1); echo y | redis-check-aof --fix "$f"' | tail -2
     $C start redis >/dev/null 2>&1; sleep 3; R --scan --pattern 'scenario:aof:*' | sed 's/^/UNLINK /' | R >/dev/null;;
  9) $CS start redis-master >/dev/null 2>&1; sleep 3
     for s in sentinel-1 sentinel-2 sentinel-3; do docker exec $s redis-cli -p 26379 sentinel set mymaster quorum 2 >/dev/null; done;;
  10) $CC start >/dev/null 2>&1; sleep 8;;
  11) R acl deluser scenario-app >/dev/null; R acl log reset >/dev/null;;
  12) $CS start redis-replica-1 >/dev/null 2>&1; sleep 5;;
  *) echo "未知情境 $1"; exit 1;;
esac; echo "情境 $1 已還原"; }

case "${1:-}" in
  list) for k in $(printf '%s\n' "${!DESC[@]}" | sort -n); do printf '  S%-3s %s\n' "$k" "${DESC[$k]}"; done;;
  inject) inject "${2:?編號}";;
  reset) reset "${2:?編號}";;
  *) sed -n '2,9p' "$0";;
esac
