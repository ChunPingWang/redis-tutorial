#!/usr/bin/env bash
# 煙霧測試：用 redis-cli 走一遍 9 種資料型別 + 常見應用場景，確認 Redis 行為正確
#   ./scripts/smoke-test.sh [-h host] [-p port] [-a password]
# 對 Cluster 請用 verify-cluster.sh（本腳本的多 key 指令需要 hash tag）
source "$(dirname "$0")/lib/common.sh"; parse_conn_args "$@"
P="smoke:$$"   # 每次執行用獨立前綴，結束時清掉

title "連線"
wait_for_ping 10 && ok "PING → PONG (${REDIS_HOST}:${REDIS_PORT})" || { fail "連不上"; summary; exit 1; }

title "String：快取、計數器、TTL"
rcli set $P:str "hello" >/dev/null;             assert_eq "SET/GET" "hello" "$(rcli get $P:str)"
rcli set $P:cnt 0 >/dev/null; rcli incr $P:cnt >/dev/null; rcli incrby $P:cnt 10 >/dev/null
assert_eq "INCR/INCRBY 原子計數" "11" "$(rcli get $P:cnt)"
rcli set $P:ttl v EX 100 >/dev/null;            assert_true "SET EX：TTL 在 1–100 秒之間" "[[ $(rcli ttl $P:ttl) -gt 0 && $(rcli ttl $P:ttl) -le 100 ]]"
assert_eq "SET NX（只在不存在時設）第二次回 nil" "" "$(rcli set $P:str x NX)"
rcli mset $P:m1 a $P:m2 b >/dev/null;           assert_eq "MSET/MGET" "a b" "$(rcli mget $P:m1 $P:m2 | paste -sd' ')"

title "Hash：物件 / 購物車"
rcli hset $P:cart apple 2 banana 1 >/dev/null; rcli hincrby $P:cart apple 3 >/dev/null
assert_eq "HSET/HINCRBY" "5" "$(rcli hget $P:cart apple)"
assert_eq "HLEN" "2" "$(rcli hlen $P:cart)"

title "List：佇列 / 最近瀏覽"
rcli rpush $P:q a b c >/dev/null;               assert_eq "RPUSH/LPOP 先進先出" "a" "$(rcli lpop $P:q)"
rcli lpush $P:recent p1 p2 p3 p4 >/dev/null; rcli ltrim $P:recent 0 2 >/dev/null
assert_eq "LPUSH+LTRIM 固定長度的最近清單" "p4 p3 p2" "$(rcli lrange $P:recent 0 -1 | paste -sd' ')"

title "Set：標籤 / 共同好友"
rcli sadd $P:s1 a b c >/dev/null; rcli sadd $P:s2 b c d >/dev/null
assert_eq "SINTER 交集" "b c" "$(rcli sinter $P:s1 $P:s2 | sort | paste -sd' ')"
assert_eq "SADD 重複成員不會增加" "0" "$(rcli sadd $P:s1 a)"

title "Sorted Set：排行榜"
rcli zadd $P:lb 95 alice 87 bob 92 carol >/dev/null; rcli zincrby $P:lb 10 bob >/dev/null
assert_eq "ZREVRANGE 前兩名" "bob alice" "$(rcli zrevrange $P:lb 0 1 | paste -sd' ')"
assert_eq "ZREVRANK carol 名次（0 起算）" "2" "$(rcli zrevrank $P:lb carol)"

title "Bitmap：簽到"
rcli setbit $P:signin 0 1 >/dev/null; rcli setbit $P:signin 6 1 >/dev/null
assert_eq "BITCOUNT 簽到天數" "2" "$(rcli bitcount $P:signin)"

title "HyperLogLog：UV 估算"
for i in $(seq 1 200); do echo "PFADD $P:uv user$i"; done | rcli >/dev/null
rcli pfadd $P:uv user1 user2 >/dev/null
cnt=$(rcli pfcount $P:uv); assert_true "PFCOUNT ≈200（誤差 <2%）：$cnt" "[[ $cnt -ge 196 && $cnt -le 204 ]]"

title "Geo：附近門市"
rcli geoadd $P:geo 121.5654 25.0330 taipei101 121.5170 25.0478 main-station >/dev/null
d=$(rcli geodist $P:geo taipei101 main-station km); assert_true "GEODIST 約 5 km：$d" "awk -v d=$d 'BEGIN{exit !(d>4 && d<7)}'"

title "Stream：事件佇列 + Consumer Group"
rcli xadd $P:stream '*' type order id 1 >/dev/null; rcli xadd $P:stream '*' type order id 2 >/dev/null
assert_eq "XLEN" "2" "$(rcli xlen $P:stream)"
rcli xgroup create $P:stream g1 0 >/dev/null
n=$(rcli xreadgroup GROUP g1 c1 COUNT 10 STREAMS $P:stream '>' | grep -c "^type$")
assert_eq "XREADGROUP 讀到 2 筆" "2" "$n"
assert_eq "XPENDING 未 ACK 2 筆" "2" "$(rcli xpending $P:stream g1 | head -1)"

title "交易與 Lua"
r=$(printf 'MULTI\nINCR %s:tx\nINCR %s:tx\nEXEC\n' $P $P | rcli | tail -2 | paste -sd' ')
assert_eq "MULTI/EXEC 回傳 1 2" "1 2" "$r"
v=$(rcli eval "return redis.call('incrby', KEYS[1], ARGV[1])" 1 $P:lua 5); assert_eq "EVAL Lua 原子加 5" "5" "$v"

title "應用場景：分散式鎖 / 限流"
assert_eq "SET NX PX 取鎖成功" "OK" "$(rcli set $P:lock owner1 NX PX 30000)"
assert_eq "第二個人取鎖失敗" "" "$(rcli set $P:lock owner2 NX PX 30000)"
# 只有持有者能釋放：比對 value 再刪（Lua 保證原子）
rel=$(rcli eval "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end" 1 $P:lock owner2)
assert_eq "非持有者釋放鎖 → 0" "0" "$rel"
rel=$(rcli eval "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end" 1 $P:lock owner1)
assert_eq "持有者釋放鎖 → 1" "1" "$rel"
for i in 1 2 3 4 5; do rcli incr $P:rate >/dev/null; done; rcli expire $P:rate 60 >/dev/null
assert_true "固定視窗限流：60 秒內第 6 次超過上限 5" "[[ $(rcli incr $P:rate) -gt 5 ]]"

title "清理"
keys=$(rcli --scan --pattern "$P:*" | wc -l)
# 用 SCAN 找出 key 再逐一 DEL（正式環境不要用 KEYS *，會卡住整台 Redis）
rcli --scan --pattern "$P:*" | sed 's/^/DEL /' | rcli >/dev/null
assert_eq "刪除測試 key（$keys 個）後 0 殘留" "0" "$(rcli --scan --pattern "$P:*" | wc -l)"
summary
