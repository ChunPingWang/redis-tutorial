#!/usr/bin/env bash
# 一鍵跑完三套容器環境的驗證（單機 → Sentinel → Cluster），每套跑完就關掉；約 3–4 分鐘
#   ./scripts/verify-all.sh
cd "$(dirname "$0")/.."; set -o pipefail
r=0
run_env() {  # <名稱> <compose 檔> <驗證腳本> [compose up 額外參數]
  local name=$1 file=$2 script=$3; shift 3
  printf '\n\033[1;35m######## %s ########\033[0m\n' "$name"
  docker compose -f "$file" up -d "$@" >/dev/null 2>&1 || { echo "compose up 失敗"; r=1; return; }
  "$script" || r=1
  docker compose -f "$file" down -v >/dev/null 2>&1
}
run_env "單機（docker-compose.yml）"            docker-compose.yml          scripts/verify-single.sh redis redis-insight redis-exporter
run_env "Sentinel（docker-compose-sentinel.yml）" docker-compose-sentinel.yml scripts/verify-sentinel.sh
run_env "Cluster（docker-compose-cluster.yml）"   docker-compose-cluster.yml  scripts/verify-cluster.sh
printf '\n\033[1;35m######## 總結：%s ########\033[0m\n' "$([[ $r == 0 ]] && echo 全部通過 || echo 有失敗)"
exit $r
