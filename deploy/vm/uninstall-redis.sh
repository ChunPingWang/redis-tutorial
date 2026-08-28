#!/usr/bin/env bash
# 移除 install-redis.sh 安裝的所有東西（練習完還原用）
#   sudo ./uninstall-redis.sh          # 保留資料目錄
#   sudo ./uninstall-redis.sh --purge  # 連 /var/lib/redis 一起刪
set -uo pipefail
[[ $EUID -eq 0 ]] || { echo "請用 sudo 執行"; exit 1; }
PURGE=0; [[ "${1:-}" == "--purge" ]] && PURGE=1
systemctl disable --now redis.service 2>/dev/null
systemctl disable --now disable-thp.service 2>/dev/null
rm -f /etc/systemd/system/redis.service /etc/systemd/system/disable-thp.service
systemctl daemon-reload
rm -f /etc/sysctl.d/99-redis.conf; sysctl --system >/dev/null 2>&1
rm -rf /opt/redis
for b in redis-server redis-cli redis-benchmark redis-check-aof redis-check-rdb; do
  [[ -L /usr/local/bin/$b ]] && rm -f /usr/local/bin/$b
done
if command -v apt-get >/dev/null && dpkg -l redis 2>/dev/null | grep -q '^ii'; then
  systemctl unmask redis-server.service 2>/dev/null; apt-get remove -y -qq redis redis-server redis-tools >/dev/null 2>&1
fi
rm -rf /etc/redis /var/log/redis /run/redis
if [[ $PURGE == 1 ]]; then rm -rf /var/lib/redis; userdel redis 2>/dev/null; fi
echo "已移除 Redis（$([[ $PURGE == 1 ]] && echo '含資料' || echo '資料目錄 /var/lib/redis 保留')）"
