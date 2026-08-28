#!/usr/bin/env bash
# 驗證 docker-compose-cluster.yml：16384 slot 全覆蓋、MOVED 重新導向、hash tag、殺掉一個 Master 後 replica 接手
#   docker compose -f docker-compose-cluster.yml up -d && ./scripts/verify-cluster.sh
cd "$(dirname "$0")/.."; source scripts/lib/common.sh
C="docker compose -f docker-compose-cluster.yml"
ccli() { docker exec redis-node-1 redis-cli -p 7001 "$@"; }       # 不帶 -c：看得到 MOVED
ccli_c() { docker exec redis-node-1 redis-cli -c -p 7001 "$@"; }  # 帶 -c：自動跟隨重新導向

title "叢集狀態"
for _ in $(seq 1 30); do [[ "$(ccli cluster info 2>/dev/null | tr -d '\r' | awk -F: '/^cluster_state/{print $2}')" == ok ]] && break; sleep 1; done
assert_eq "cluster_state ok" "ok" "$(ccli cluster info | tr -d '\r' | awk -F: '/^cluster_state/{print $2}')"
assert_eq "16384 個 slot 全部指派" "16384" "$(ccli cluster info | tr -d '\r' | awk -F: '/^cluster_slots_assigned/{print $2}')"
assert_eq "6 個已知節點" "6" "$(ccli cluster info | tr -d '\r' | awk -F: '/^cluster_known_nodes/{print $2}')"
assert_eq "3 個 Master" "3" "$(ccli cluster nodes | grep -c master)"
assert_eq "3 個 Replica" "3" "$(ccli cluster nodes | grep -c slave)"

title "Hash Slot 與重新導向"
info "CLUSTER KEYSLOT user:1001 = $(ccli cluster keyslot user:1001)"
assert_eq "hash tag {user:1001} 的兩個 key 落在同一 slot" "$(ccli cluster keyslot '{user:1001}:profile')" "$(ccli cluster keyslot '{user:1001}:cart')"
moved=0; for k in a b c d e f g h; do ccli set "verify:$k" 1 2>&1 | grep -q MOVED && moved=$((moved+1)); done
assert_true "不帶 -c 寫 8 個 key，有 $moved 個被 MOVED 重新導向（分片生效）" "[[ $moved -gt 0 ]]"
okc=0; for k in a b c d e f g h; do [[ "$(ccli_c set "verify:$k" 1)" == OK ]] && okc=$((okc+1)); done
assert_eq "帶 -c 寫 8 個 key 全部成功" "8" "$okc"
assert_eq "MGET 不同 slot 的 key 會被拒（CROSSSLOT）" "CROSSSLOT" "$(ccli_c mget verify:a verify:b 2>&1 | awk '{print $1}' | tr -d '()')"
ccli_c mset '{order:1}:a' 1 '{order:1}:b' 2 >/dev/null; assert_eq "同 hash tag 的 MSET/MGET 可以" "1 2" "$(ccli_c mget '{order:1}:a' '{order:1}:b' | paste -sd' ')"

title "故障轉移：停掉一個 Master，它的 Replica 接手"
victim_ip=$(ccli cluster nodes | awk '/master/ && !/myself/ {print $2; exit}' | cut -d: -f1)
victim_id=$(ccli cluster nodes | awk -v v="$victim_ip" '$2 ~ v {print $1}')
victim="redis-node-${victim_ip##*.1}"      # 172.29.0.1N → redis-node-N
$C stop "$victim" >/dev/null 2>&1
t0=$(date +%s); promoted=""
for _ in $(seq 1 40); do
  promoted=$(ccli cluster nodes | awk -v id="$victim_id" '$1!=id && /master/ && !/fail/ {n++} END{print n}')
  [[ "$promoted" == 3 ]] && break; sleep 1
done
assert_eq "停掉 $victim 後仍有 3 個可用 Master（$(( $(date +%s)-t0 ))s）" "3" "$promoted"
assert_eq "cluster_state 仍為 ok" "ok" "$(ccli cluster info | tr -d '\r' | awk -F: '/^cluster_state/{print $2}')"
assert_eq "資料仍讀得到" "1" "$(ccli_c get verify:a)"
$C start "$victim" >/dev/null 2>&1; sleep 5
assert_eq "$victim 回歸後變成 slave" "1" "$(ccli cluster nodes | awk -v id="$victim_id" '$1==id && /slave/ {print 1}')"
for k in a b c d e f g h; do ccli_c del "verify:$k" >/dev/null; done; ccli_c del '{order:1}:a' '{order:1}:b' >/dev/null
summary
