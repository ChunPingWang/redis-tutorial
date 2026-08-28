#!/usr/bin/env bash
# 所有腳本共用的工具函式。用法：source "$(dirname "$0")/lib/common.sh"
#
# 連線參數可用環境變數或各腳本的 -h/-p/-a 覆寫：
#   REDIS_HOST（預設 127.0.0.1） REDIS_PORT（6379） REDIS_PASSWORD（空）
# 沒有本機 redis-cli 時自動改用 redis:8-alpine 容器裡的 redis-cli（--network host）。

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
PASS=0; FAIL=0

c_blue='\033[1;34m'; c_green='\033[1;32m'; c_red='\033[1;31m'; c_yellow='\033[1;33m'; c_off='\033[0m'
title() { printf "\n${c_blue}== %s ==${c_off}\n" "$*"; }
ok()    { PASS=$((PASS+1)); printf "${c_green}  ✔ %s${c_off}\n" "$*"; }
fail()  { FAIL=$((FAIL+1)); printf "${c_red}  ✘ %s${c_off}\n" "$*"; }
warn()  { printf "${c_yellow}  ⚠ %s${c_off}\n" "$*"; }
info()  { printf "    %s\n" "$*"; }

# 找 redis-cli：本機 → 容器
if command -v redis-cli >/dev/null 2>&1; then
  _CLI=(redis-cli)
else
  _CLI=(docker run --rm -i --network host redis:8-alpine redis-cli)
fi

# rcli <args...>：帶上 host/port/password 呼叫 redis-cli
rcli() {
  local auth=()
  [[ -n "$REDIS_PASSWORD" ]] && auth=(-a "$REDIS_PASSWORD" --no-auth-warning)
  "${_CLI[@]}" -h "$REDIS_HOST" -p "$REDIS_PORT" "${auth[@]}" "$@"
}

# assert_eq <說明> <期望> <實際>
assert_eq() {
  if [[ "$2" == "$3" ]]; then ok "$1"; else fail "$1（期望 '$2'，實際 '$3'）"; fi
}
# assert_true <說明> <shell 條件...>
assert_true() { local d="$1"; shift; if eval "$@"; then ok "$d"; else fail "$d"; fi; }

# wait_for_ping <秒數>
wait_for_ping() {
  local n="${1:-30}"
  for _ in $(seq 1 "$n"); do [[ "$(rcli ping 2>/dev/null)" == PONG ]] && return 0; sleep 1; done
  return 1
}

summary() {
  printf "\n${c_blue}結果：${c_off}${c_green}%d 通過${c_off}，${c_red}%d 失敗${c_off}\n" "$PASS" "$FAIL"
  [[ "$FAIL" -eq 0 ]]
}

parse_conn_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -h) REDIS_HOST="$2"; shift 2;;
      -p) REDIS_PORT="$2"; shift 2;;
      -a) REDIS_PASSWORD="$2"; shift 2;;
      *) shift;;
    esac
  done
}
