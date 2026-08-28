#!/usr/bin/env bash
# 驗證 docker-compose.yml 單機環境：容器健康、設定生效、資料重啟不丟、監控端點正常
#   docker compose up -d && ./scripts/verify-single.sh
cd "$(dirname "$0")/.."; source scripts/lib/common.sh
REDIS_PORT=6379

title "容器狀態"
assert_eq "redis 容器 healthy" "healthy" "$(docker inspect -f '{{.State.Health.Status}}' redis-tutorial 2>/dev/null)"
wait_for_ping 30 && ok "PING → PONG" || { fail "連不上 127.0.0.1:6379"; summary; exit 1; }

title "設定檔生效（docker/redis/redis.conf）"
assert_eq "maxmemory 256mb" "268435456" "$(rcli config get maxmemory | tail -1)"
assert_eq "maxmemory-policy allkeys-lru" "allkeys-lru" "$(rcli config get maxmemory-policy | tail -1)"
assert_eq "appendonly yes" "yes" "$(rcli config get appendonly | tail -1)"
assert_eq "aof-use-rdb-preamble yes（混合持久化）" "yes" "$(rcli config get aof-use-rdb-preamble | tail -1)"
mods=$(rcli module list | grep -c '^name$'); assert_true "redis:8 內建模組已載入（$mods 個：JSON/Search/Bloom/TimeSeries/VectorSet）" "[[ $mods -ge 4 ]]"
assert_eq "JSON.SET/GET 可用" '{"a":1}' "$(rcli json.set verify:json '$' '{"a":1}' >/dev/null && rcli json.get verify:json)"
rcli del verify:json >/dev/null

title "資料持久化：重啟容器後資料仍在"
rcli set verify:persist "survived-$$" >/dev/null
docker compose restart redis >/dev/null 2>&1; wait_for_ping 30
assert_eq "重啟後 GET 仍有值（AOF 生效）" "survived-$$" "$(rcli get verify:persist)"
rcli del verify:persist >/dev/null

title "監控端點"
running() { docker compose ps --services --status running 2>/dev/null | grep -qx "$1"; }   # 只看本 compose 專案的服務
if running redis-insight; then
  code=000; for _ in $(seq 1 60); do code=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:5540/ 2>/dev/null); [[ $code == 200 ]] && break; sleep 1; done
  assert_true "RedisInsight http://localhost:5540 回應 $code" "[[ $code == 200 ]]"
else warn "RedisInsight 未啟動（docker compose up -d redis-insight）"; fi
if running redis-exporter; then
  for _ in $(seq 1 15); do curl -s http://localhost:9121/metrics 2>/dev/null | grep -q '^redis_up 1' && break; sleep 1; done
  assert_true "redis_exporter 有 redis_up 1" "curl -s http://localhost:9121/metrics | grep -q '^redis_up 1'"
else warn "redis-exporter 未啟動（只跑了 redis 的話屬正常）"; fi
if running prometheus; then
  pport=$(docker compose port prometheus 9090 | awk -F: '{print $NF}')
  for _ in $(seq 1 30); do curl -s "http://localhost:$pport/api/v1/targets" 2>/dev/null | grep -q '"health":"up"' && break; sleep 1; done
  assert_true "Prometheus（:$pport）抓到 redis target 且 health=up" "curl -s http://localhost:$pport/api/v1/targets | grep -q '\"job\":\"redis\"'"
fi
if running grafana; then
  gport=$(docker compose port grafana 3000 | awk -F: '{print $NF}')
  for _ in $(seq 1 30); do curl -s -u admin:admin "http://localhost:$gport/api/dashboards/uid/redis-tutorial" 2>/dev/null | grep -q '"title":"Redis Tutorial"' && break; sleep 1; done
  assert_true "Grafana（:$gport）已自動載入 Redis Tutorial 儀表板" "curl -s -u admin:admin http://localhost:$gport/api/dashboards/uid/redis-tutorial | grep -q '\"title\":\"Redis Tutorial\"'"
fi
summary
