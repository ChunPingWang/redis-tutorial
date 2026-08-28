#!/usr/bin/env bash
# 線上備份：觸發 BGSAVE、等它完成、把 RDB 複製到備份目錄（含時間戳）；可搭配 cron
#   ./scripts/backup.sh [-h host] [-p port] [-a password] [-d 備份目錄]
#   還原：停 Redis → 把 .rdb 放回 dir/dbfilename → 若開 AOF 需先 redis-cli DEBUG... 見 docs/06-operations.md
source "$(dirname "$0")/lib/common.sh"; parse_conn_args "$@"
DEST="./backups"; while [[ $# -gt 0 ]]; do case "$1" in -d) DEST="$2"; shift 2;; *) shift;; esac; done
mkdir -p "$DEST"
title "BGSAVE 到 ${REDIS_HOST}:${REDIS_PORT}"
rcli bgsave >/dev/null || { fail "BGSAVE 失敗"; exit 1; }
# 等背景存檔結束（rdb_bgsave_in_progress 回到 0）；LASTSAVE 只有秒級精度，不適合拿來等
sleep 0.5
for _ in $(seq 1 600); do [[ "$(rcli info persistence | tr -d '\r' | awk -F: '/^rdb_bgsave_in_progress/{print $2}')" == 0 ]] && break; sleep 0.5; done
[[ "$(rcli info persistence | tr -d '\r' | awk -F: '/^rdb_last_bgsave_status/{print $2}')" == ok ]] && ok "BGSAVE 完成" || { fail "BGSAVE 狀態不是 ok"; exit 1; }
dir=$(rcli config get dir | tail -1); file=$(rcli config get dbfilename | tail -1)
out="$DEST/dump-$(date +%Y%m%d-%H%M%S).rdb"
if [[ -r "$dir/$file" ]]; then cp "$dir/$file" "$out"
elif [[ "$REDIS_HOST" == 127.0.0.1 || "$REDIS_HOST" == localhost ]] && sudo -n true 2>/dev/null; then sudo cp "$dir/$file" "$out" && sudo chown "$USER" "$out"
else
  # Redis 在遠端或容器裡：用 --rdb 透過複寫協定把 RDB 拉下來
  rcli --rdb "$out" >/dev/null 2>&1
fi
[[ -s "$out" ]] && ok "備份檔：$out（$(du -h "$out" | cut -f1)）" || { fail "取不到 RDB 檔"; exit 1; }
if command -v redis-check-rdb >/dev/null; then redis-check-rdb "$out" >/dev/null 2>&1 && ok "redis-check-rdb 檢查通過" || fail "RDB 檔損毀"; fi
ls -1t "$DEST"/dump-*.rdb 2>/dev/null | tail -n +8 | xargs -r rm -f; info "保留最近 7 份"
