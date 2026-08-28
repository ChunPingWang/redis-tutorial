#!/usr/bin/env bash
# 驗證 docker-compose-sentinel.yml：拓撲正確 → 停掉 Master → Sentinel 自動 failover → 資料保留 → 舊 Master 回來變 Replica
#   docker compose -f docker-compose-sentinel.yml up -d && ./scripts/verify-sentinel.sh
cd "$(dirname "$0")/.."; source scripts/lib/common.sh
C="docker compose -f docker-compose-sentinel.yml"
scli() { docker exec sentinel-1 redis-cli -p 26379 "$@"; }   # 透過 sentinel-1 查詢
master_addr() { scli SENTINEL get-master-addr-by-name mymaster | paste -sd: ; }
rcli_ip() { local ip=$1; shift; docker run --rm --network redis-sentinel_sentinel-net redis:8-alpine redis-cli -h "$ip" "$@"; }

title "拓撲：1 Master + 2 Replica + 3 Sentinel"
for c in redis-master redis-replica-1 redis-replica-2 sentinel-1 sentinel-2 sentinel-3; do
  assert_eq "$c 執行中" "running" "$(docker inspect -f '{{.State.Status}}' $c 2>/dev/null)"
done
# Sentinel 靠 INFO（每 10 秒）與 hello 頻道（每 2 秒）發現節點，剛啟動要等一下
for _ in $(seq 1 60); do
  [[ "$(scli SENTINEL sentinels mymaster 2>/dev/null | grep -c '^ip$')" == 2 && "$(scli SENTINEL replicas mymaster 2>/dev/null | grep -c '^ip$')" == 2 ]] && break; sleep 1
done
assert_eq "Sentinel 看到的 Master 是 172.28.0.10:6379" "172.28.0.10:6379" "$(master_addr)"
assert_eq "Sentinel 互相發現（其他 2 個）" "2" "$(scli SENTINEL sentinels mymaster | grep -c '^ip$')"
assert_eq "Sentinel 發現 2 個 Replica" "2" "$(scli SENTINEL replicas mymaster | grep -c '^ip$')"
assert_eq "Master 有 2 個 replica 連線" "2" "$(docker exec redis-master redis-cli info replication | tr -d '\r' | awk -F: '/^connected_slaves/{print $2}')"

title "複寫：寫 Master，Replica 讀得到"
docker exec redis-master redis-cli set verify:repl "from-master-$$" >/dev/null; sleep 1
assert_eq "replica-1 讀到" "from-master-$$" "$(docker exec redis-replica-1 redis-cli get verify:repl)"
assert_eq "replica 唯讀（寫入被拒）" "READONLY" "$(docker exec redis-replica-1 redis-cli set x 1 2>&1 | awk '{print $1}' | tr -d '()')"

title "自動故障轉移：停掉 Master"
old=$(master_addr); $C stop redis-master >/dev/null 2>&1
t0=$(date +%s); new=""
for _ in $(seq 1 40); do new=$(master_addr); [[ "$new" != "$old" ]] && break; sleep 1; done
t=$(( $(date +%s) - t0 ))
assert_true "Sentinel 在 ${t}s 內切換 Master：$old → $new" "[[ '$new' != '$old' ]]"
docker logs sentinel-1 2>&1 | grep -q '+switch-master' && ok "sentinel-1 日誌有 +switch-master" || fail "沒看到 +switch-master"
newip=${new%%:*}
assert_eq "新 Master 保有資料" "from-master-$$" "$(rcli_ip $newip get verify:repl)"
assert_eq "新 Master 可寫入" "OK" "$(rcli_ip $newip set verify:after-failover 1)"

title "舊 Master 回歸 → 被降為 Replica"
$C start redis-master >/dev/null 2>&1
role=""; for _ in $(seq 1 30); do role=$(docker exec redis-master redis-cli info replication 2>/dev/null | tr -d '\r' | awk -F: '/^role/{print $2}'); [[ "$role" == slave ]] && break; sleep 1; done
assert_eq "舊 Master 角色變成 slave" "slave" "$role"
# 變成 slave 後還要完成一次全量同步（master_link_status:up）才讀得到新資料
for _ in $(seq 1 30); do [[ "$(docker exec redis-master redis-cli info replication | tr -d '\r' | awk -F: '/^master_link_status/{print $2}')" == up ]] && break; sleep 1; done
assert_eq "舊 Master 完成同步、追上新 Master 的資料" "1" "$(docker exec redis-master redis-cli get verify:after-failover)"
rcli_ip $newip del verify:repl verify:after-failover >/dev/null
summary
