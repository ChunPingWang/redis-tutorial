#!/usr/bin/env bash
# 驗證 deploy/vm/install-redis.sh 的成果：systemd、設定、OS 調校、graceful restart 不丟資料
#   sudo ./scripts/verify-vm.sh -a <密碼> [-p 6379]
cd "$(dirname "$0")/.."; source scripts/lib/common.sh; parse_conn_args "$@"
[[ $EUID -eq 0 ]] || { echo "請用 sudo 執行（要讀 /etc/redis/redis.conf 與 systemd 狀態）"; exit 1; }

title "systemd"
assert_eq "redis.service active" "active" "$(systemctl is-active redis)"
assert_eq "redis.service enabled（開機自啟）" "enabled" "$(systemctl is-enabled redis)"
assert_eq "Type=notify（Redis 載入完資料才算啟動成功）" "Type=notify" "$(systemctl show redis -p Type)"
assert_eq "以 redis 帳號執行" "User=redis" "$(systemctl show redis -p User)"
assert_eq "LimitNOFILE=65536" "LimitNOFILE=65536" "$(systemctl show redis -p LimitNOFILE)"
assert_eq "行程確實以 redis 使用者跑" "redis" "$(ps -o user= -p "$(systemctl show redis -p MainPID --value)" | tr -d ' ')"

title "設定與檔案權限"
assert_eq "/etc/redis/redis.conf 權限 640（含密碼）" "640" "$(stat -c %a /etc/redis/redis.conf)"
assert_eq "資料目錄屬於 redis" "redis" "$(stat -c %U /var/lib/redis)"
assert_eq "supervised systemd" "systemd" "$(rcli config get supervised | tail -1)"
assert_true "appendonly yes" "[[ $(rcli config get appendonly | tail -1) == yes ]]"
assert_true "maxmemory 已設定（非 0）" "[[ $(rcli config get maxmemory | tail -1) != 0 ]]"
assert_eq "bind 127.0.0.1（不對外）" "127.0.0.1" "$(rcli config get bind | tail -1)"
assert_eq "沒密碼被拒（NOAUTH）" "NOAUTH" "$(redis-cli -p "$REDIS_PORT" get x 2>&1 | awk '{print $1}' | tr -d '()')"

title "OS 調校"
assert_eq "vm.overcommit_memory=1" "1" "$(sysctl -n vm.overcommit_memory)"
assert_true "net.core.somaxconn ≥ 1024" "[[ $(sysctl -n net.core.somaxconn) -ge 1024 ]]"
if [[ -f /sys/kernel/mm/transparent_hugepage/enabled ]]; then
  assert_true "THP = never" "grep -q '\[never\]' /sys/kernel/mm/transparent_hugepage/enabled"
fi
grep -qi "WARNING" /var/log/redis/redis.log && fail "redis.log 有 WARNING：$(grep -i WARNING /var/log/redis/redis.log | tail -1)" || ok "redis.log 沒有 WARNING"

title "Graceful restart：重啟服務後資料仍在"
rcli set verify:vm:restart "before-$$" >/dev/null
t0=$(date +%s%N); systemctl restart redis; wait_for_ping 30; t=$(( ($(date +%s%N)-t0)/1000000 ))
assert_eq "restart 後資料仍在（${t}ms 恢復服務）" "before-$$" "$(rcli get verify:vm:restart)"
assert_eq "restart 次數（NRestarts，非預期重啟）為 0" "NRestarts=0" "$(systemctl show redis -p NRestarts)"
rcli del verify:vm:restart >/dev/null
summary
