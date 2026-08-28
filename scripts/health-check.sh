#!/usr/bin/env bash
# Redis 健康檢查：一眼看出這台 Redis 現在健不健康
#   ./scripts/health-check.sh                    # 127.0.0.1:6379
#   ./scripts/health-check.sh -p 6380 -a secret  # 指定 port / 密碼
# 回傳值：0 健康、1 有失敗項目（可接到監控或 CI）
source "$(dirname "$0")/lib/common.sh"; parse_conn_args "$@"

title "連線 ${REDIS_HOST}:${REDIS_PORT}"
[[ "$(rcli ping 2>/dev/null)" == PONG ]] && ok "PING → PONG" || { fail "連不上"; summary; exit 1; }

INFO="$(rcli info 2>/dev/null | tr -d '\r')"
get() { echo "$INFO" | awk -F: -v k="$1" '$1==k{print $2}'; }

title "基本資訊"
info "版本 $(get redis_version)   模式 $(get redis_mode)   角色 $(get role)   uptime $(get uptime_in_days) 天"
info "OS $(get os)   記憶體配置器 $(get mem_allocator)"

title "記憶體"
used=$(get used_memory); maxm=$(get maxmemory); frag=$(get mem_fragmentation_ratio)
info "used_memory_human=$(get used_memory_human)  maxmemory_human=$(get maxmemory_human)  policy=$(get maxmemory_policy)"
if [[ "$maxm" == 0 ]]; then warn "maxmemory 未設定：Redis 可能吃光整台機器的記憶體"; else
  pct=$(( used * 100 / maxm )); [[ $pct -lt 90 ]] && ok "記憶體使用率 ${pct}%" || fail "記憶體使用率 ${pct}%（>90%，開始淘汰或拒寫）"
fi
# fragmentation：<1 表示已用 swap（糟），>1.5 表示碎片多（考慮 activedefrag）；資料 <10MB 時這個比值沒有意義
if [[ "$used" -lt 10485760 ]]; then info "碎片率 ${frag}（資料量 <10MB，此指標不具參考價值）"
elif awk -v f="$frag" 'BEGIN{exit !(f>=1.0 && f<=1.5)}'; then ok "碎片率 ${frag}"
else warn "碎片率 ${frag}（正常 1.0–1.5；<1 可能用到 swap，>1.5 可開 activedefrag）"; fi

title "持久化"
[[ "$(get rdb_last_bgsave_status)" == ok ]] && ok "最近一次 RDB bgsave 成功" || fail "RDB bgsave 失敗：$(get rdb_last_bgsave_status)"
if [[ "$(get aof_enabled)" == 1 ]]; then
  [[ "$(get aof_last_bgrewrite_status)" == ok ]] && ok "AOF 開啟，最近一次 rewrite 成功" || fail "AOF rewrite 失敗"
  [[ "$(get aof_last_write_status)" == ok ]] && ok "AOF 寫入正常" || fail "AOF 寫入失敗：$(get aof_last_write_status)"
else warn "AOF 未開啟（純快取可接受；有資料就該開）"; fi

title "客戶端與效能"
info "connected_clients=$(get connected_clients)  blocked_clients=$(get blocked_clients)  rejected_connections=$(get rejected_connections)"
info "instantaneous_ops_per_sec=$(get instantaneous_ops_per_sec)  total_commands_processed=$(get total_commands_processed)"
hits=$(get keyspace_hits); misses=$(get keyspace_misses)
if [[ $((hits+misses)) -gt 0 ]]; then info "快取命中率 $(( hits * 100 / (hits+misses) ))%（hits=$hits misses=$misses）"; fi
info "evicted_keys=$(get evicted_keys)  expired_keys=$(get expired_keys)"
[[ "$(get rejected_connections)" == 0 ]] && ok "沒有被拒絕的連線" || fail "有被拒絕的連線（maxclients 或 ulimit 不夠）"
slow=$(rcli slowlog len 2>/dev/null); [[ "$slow" -eq 0 ]] && ok "slowlog 為空" || warn "slowlog 有 ${slow} 筆（redis-cli slowlog get 10 查看）"

title "複寫"
role=$(get role)
if [[ "$role" == master ]]; then info "connected_slaves=$(get connected_slaves)"; ok "角色 master"
else
  [[ "$(get master_link_status)" == up ]] && ok "replica，與 master $(get master_host):$(get master_port) 連線中" || fail "replica 與 master 斷線"
fi

title "Keyspace"
echo "$INFO" | grep -E "^db[0-9]+:" | sed 's/^/    /' || info "（沒有資料）"
summary
