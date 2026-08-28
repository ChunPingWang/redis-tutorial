#!/usr/bin/env bash
# 用 redis-benchmark 量測：一般 vs pipeline、不同 value 大小；讀完 docs/04-performance-tuning.md 再來解讀數字
#   ./scripts/benchmark.sh [-h host] [-p port] [-a password] [-n 請求數]
source "$(dirname "$0")/lib/common.sh"; parse_conn_args "$@"
N=100000; while [[ $# -gt 0 ]]; do case "$1" in -n) N="$2"; shift 2;; *) shift;; esac; done
if command -v redis-benchmark >/dev/null; then BENCH=(redis-benchmark); else BENCH=(docker run --rm --network host redis:8-alpine redis-benchmark); fi
auth=(); [[ -n "$REDIS_PASSWORD" ]] && auth=(-a "$REDIS_PASSWORD")
# -q 的進度列用 \r 覆寫，改成只留最後一行結果
b() { "${BENCH[@]}" -h "$REDIS_HOST" -p "$REDIS_PORT" "${auth[@]}" -q -n "$N" "$@" 2>/dev/null | tr '\r' '\n' | grep -E "requests per second" | sed 's/^/  /'; }

title "1. 基準：50 個連線、無 pipeline、value 3 bytes（-n $N）"
b -c 50 -t set,get,incr,lpush,rpop,sadd,zadd -r 100000
title "2. Pipeline 16：同樣的指令，一次送 16 個（看吞吐量差幾倍）"
b -c 50 -t set,get -P 16 -r 100000
title "3. value 大小的影響：1KB vs 100KB（大 value = 網路與記憶體頻寬瓶頸）"
echo "  -- 1KB";   b -c 50 -t set,get -d 1024 -r 100000
echo "  -- 100KB"; b -c 50 -t set,get -d 102400 -n $((N/10)) -r 1000
title "4. 延遲分佈（-c 1：單一連線的往返時間）"
"${BENCH[@]}" -h "$REDIS_HOST" -p "$REDIS_PORT" "${auth[@]}" -n 10000 -c 1 -t get --precision 3 2>/dev/null | tr '' '
' | grep -A2 "latency summary" | sed 's/^/  /'
title "清理 benchmark 產生的 key"
rcli --scan --pattern 'key:*' | sed 's/^/DEL /' | rcli >/dev/null; rcli del myset mylist myzset counter:__rand_int__ >/dev/null 2>&1
ok "完成"
