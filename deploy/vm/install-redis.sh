#!/usr/bin/env bash
# ============================================================
# Redis 8 VM 一鍵佈署腳本
#
#   sudo ./install-redis.sh                       # 自動選擇安裝方式，全預設
#   sudo ./install-redis.sh --method source       # 強制原始碼編譯（任何 Linux 都適用）
#   sudo ./install-redis.sh --password 'S3cret!' --bind 10.0.0.5 --maxmemory 4gb
#   DRY_RUN=1 ./install-redis.sh                  # 只印出會做什麼
#
# 做七件事（對應 docs/03-deploy-vm.md 的手動 runbook）：
#   1. preflight：權限、port、必要工具
#   2. 建立 redis 系統帳號與目錄（/etc/redis、/var/lib/redis、/var/log/redis）
#   3. 安裝 Redis：apt（packages.redis.io 官方倉庫）/ dnf（發行版套件）/ source（編譯）
#   4. 由範本渲染 /etc/redis/redis.conf
#   5. OS 調校：sysctl、關閉 THP
#   6. 安裝並啟動 systemd 服務
#   7. 驗證：ping、讀寫、持久化、版本
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REDIS_VERSION="${REDIS_VERSION:-8.10.1}"
METHOD="auto"            # auto | apt | dnf | source
PREFIX="/opt/redis"      # source 安裝路徑；apt/dnf 會自動改為 /usr
PORT=6379
BIND="127.0.0.1"
PASSWORD=""
MAXMEMORY="1gb"
DATA_DIR="/var/lib/redis"
LOG_DIR="/var/log/redis"
CONF_DIR="/etc/redis"
SKIP_TUNING=0
NO_SYSTEMD=0             # 容器內測試用：不裝 systemd unit，改以背景行程啟動
DRY_RUN="${DRY_RUN:-0}"

usage() { sed -n '2,20p' "$0"; exit 0; }
while [[ $# -gt 0 ]]; do
  case "$1" in
    --method) METHOD="$2"; shift 2;;
    --version) REDIS_VERSION="$2"; shift 2;;
    --prefix) PREFIX="$2"; shift 2;;
    --port) PORT="$2"; shift 2;;
    --bind) BIND="$2"; shift 2;;
    --password) PASSWORD="$2"; shift 2;;
    --maxmemory) MAXMEMORY="$2"; shift 2;;
    --data-dir) DATA_DIR="$2"; shift 2;;
    --skip-tuning) SKIP_TUNING=1; shift;;
    --no-systemd) NO_SYSTEMD=1; shift;;
    --dry-run) DRY_RUN=1; shift;;
    -h|--help) usage;;
    *) echo "未知參數: $1"; usage;;
  esac
done

# ---------- 小工具 ----------
log()  { printf '\033[1;34m[%s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*"; }
ok()   { printf '\033[1;32m  ✔ %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m  ⚠ %s\033[0m\n' "$*"; }
die()  { printf '\033[1;31m  ✘ %s\033[0m\n' "$*" >&2; exit 1; }
run()  { if [[ "$DRY_RUN" == 1 ]]; then echo "  [dry-run] $*"; else "$@"; fi; }

# ---------- 1. preflight ----------
log "1/7 preflight 前置檢查"
[[ $EUID -eq 0 ]] || die "請用 root 或 sudo 執行"
. /etc/os-release
OS_ID="${ID:-unknown}"; OS_LIKE="${ID_LIKE:-}"
ok "作業系統：${PRETTY_NAME:-$OS_ID}"

if command -v ss >/dev/null && ss -tln 2>/dev/null | grep -qE "[:.]${PORT}\b"; then
  die "port ${PORT} 已被占用（ss -tlnp | grep ${PORT} 查看是誰）"
fi
ok "port ${PORT} 可用"

if [[ "$METHOD" == auto ]]; then
  if command -v apt-get >/dev/null; then METHOD=apt
  elif command -v dnf >/dev/null && dnf list --available redis >/dev/null 2>&1 && [[ "$OS_ID" != fedora ]]; then METHOD=dnf
  else METHOD=source; fi
fi
ok "安裝方式：${METHOD}"
[[ "$METHOD" == source ]] || PREFIX="/usr"

if [[ -z "$PASSWORD" ]]; then
  PASSWORD="$(openssl rand -hex 16 2>/dev/null || head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n')"
  warn "未指定 --password，已自動產生（結尾會印出）"
fi

if [[ "$NO_SYSTEMD" == 0 ]] && ! command -v systemctl >/dev/null; then
  warn "找不到 systemctl，改用 --no-systemd 模式"
  NO_SYSTEMD=1
fi

# ---------- 2. 帳號與目錄 ----------
log "2/7 建立 redis 系統帳號與目錄"
if ! getent passwd redis >/dev/null; then
  run useradd --system --home-dir "$DATA_DIR" --shell /usr/sbin/nologin --user-group redis 2>/dev/null \
    || run useradd --system --home-dir "$DATA_DIR" --shell /sbin/nologin --user-group redis
fi
run mkdir -p "$CONF_DIR" "$DATA_DIR" "$LOG_DIR"
run chown -R redis:redis "$DATA_DIR" "$LOG_DIR"
run chmod 750 "$DATA_DIR" "$LOG_DIR"
ok "redis 帳號、${CONF_DIR} ${DATA_DIR} ${LOG_DIR} 就緒"

# ---------- 3. 安裝 ----------
log "3/7 安裝 Redis（${METHOD}）"
install_apt() {
  export DEBIAN_FRONTEND=noninteractive
  run apt-get update -qq
  run apt-get install -y -qq lsb-release curl gpg ca-certificates
  if [[ ! -f /usr/share/keyrings/redis-archive-keyring.gpg ]]; then
    run bash -c 'curl -fsSL https://packages.redis.io/gpg | gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg'
    run chmod 644 /usr/share/keyrings/redis-archive-keyring.gpg
  fi
  run bash -c "echo 'deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main' > /etc/apt/sources.list.d/redis.list"
  run apt-get update -qq
  run apt-get install -y -qq redis
  # 套件自帶的 unit（redis-server.service）與本腳本的 redis.service 二選一：這裡統一用本腳本的
  if [[ "$NO_SYSTEMD" == 0 ]]; then
    run systemctl disable --now redis-server.service 2>/dev/null || true
    run systemctl mask redis-server.service 2>/dev/null || true
  fi
}
install_dnf() {
  run dnf install -y redis
  if [[ "$NO_SYSTEMD" == 0 ]]; then run systemctl disable --now redis.service 2>/dev/null || true; fi
  warn "發行版套件版本通常較舊（redis-server --version 確認）；要 Redis 8 請用 --method source"
}
install_source() {
  local tarball="redis-${REDIS_VERSION}.tar.gz" url="https://download.redis.io/releases/redis-${REDIS_VERSION}.tar.gz"
  local work="/usr/local/src"
  # 編譯依賴
  if command -v apt-get >/dev/null; then
    export DEBIAN_FRONTEND=noninteractive
    run apt-get update -qq; run apt-get install -y -qq build-essential pkg-config libssl-dev libsystemd-dev curl ca-certificates
  elif command -v dnf >/dev/null; then
    run dnf install -y gcc make pkgconf-pkg-config openssl-devel systemd-devel curl tar
  elif command -v yum >/dev/null; then
    run yum install -y gcc make pkgconfig openssl-devel systemd-devel curl tar
  fi
  run mkdir -p "$work"
  if [[ ! -f "$work/$tarball" ]]; then
    run curl -fsSL -o "$work/$tarball" "$url"
  fi
  # 驗證 SHA-256（官方 redis-hashes 倉庫）
  local expected
  expected="$(curl -fsSL https://raw.githubusercontent.com/redis/redis-hashes/master/README 2>/dev/null | awk -v t="$tarball" '$2==t && $3=="sha256"{print $4}')"
  if [[ -n "$expected" && "$DRY_RUN" == 0 ]]; then
    echo "${expected}  ${work}/${tarball}" | sha256sum -c --quiet && ok "SHA-256 驗證通過" || die "SHA-256 不符，拒絕安裝"
  elif [[ "$DRY_RUN" == 1 ]]; then
    echo "  [dry-run] sha256sum -c（官方雜湊：${expected:-取不到}）"
  else
    warn "取不到官方雜湊值，跳過 SHA-256 驗證"
  fi
  run tar -xzf "$work/$tarball" -C "$work"
  local src="$work/redis-${REDIS_VERSION}"
  local tls="no"; pkg-config --exists openssl 2>/dev/null && tls="yes"
  local sysd="no"; pkg-config --exists libsystemd 2>/dev/null && sysd="yes"
  # Redis 8.10+ 的 `make` 會連同 JSON / Search / TimeSeries 模組一起編譯，需要 LLVM 21 + Rust + CMake；
  # VM 佈署只編譯核心（`make build redis`）。要模組請改用 apt 套件或 redis:8 容器映像（見 docs/03-deploy-vm.md）。
  log "  make -j$(nproc) build redis BUILD_TLS=${tls} USE_SYSTEMD=${sysd}（核心，約 1–3 分鐘）"
  run bash -c "cd '$src' && make -j$(nproc) build redis BUILD_TLS=${tls} USE_SYSTEMD=${sysd} > /tmp/redis-build.log 2>&1" \
    || die "編譯失敗，見 /tmp/redis-build.log"
  run bash -c "cd '$src' && make install PREFIX='$PREFIX' >> /tmp/redis-build.log 2>&1"
  run mkdir -p /usr/local/bin
  for b in redis-server redis-cli redis-benchmark redis-check-aof redis-check-rdb; do
    run ln -sf "$PREFIX/bin/$b" "/usr/local/bin/$b"
  done
}
case "$METHOD" in
  apt) install_apt;; dnf) install_dnf;; source) install_source;;
  *) die "未知的 --method $METHOD";;
esac
[[ "$DRY_RUN" == 1 ]] || ok "$("$PREFIX/bin/redis-server" --version)"

# ---------- 4. 渲染設定檔 ----------
log "4/7 產生 ${CONF_DIR}/redis.conf"
CONF="$CONF_DIR/redis.conf"
if [[ -f "$CONF" && "$DRY_RUN" == 0 ]]; then cp "$CONF" "$CONF.bak.$(date +%Y%m%d%H%M%S)"; warn "已備份既有設定"; fi
render() {
  sed -e "s|__PORT__|$PORT|g" -e "s|__BIND__|$BIND|g" -e "s|__PASSWORD__|$PASSWORD|g" \
      -e "s|__MAXMEMORY__|$MAXMEMORY|g" -e "s|__DATA_DIR__|$DATA_DIR|g" -e "s|__LOG_DIR__|$LOG_DIR|g" \
      -e "s|__PREFIX__|$PREFIX|g" -e "s|__CONF__|$CONF|g" "$1"
}
if [[ "$DRY_RUN" == 1 ]]; then echo "  [dry-run] render redis.conf.template -> $CONF"; else
  render "$SCRIPT_DIR/redis.conf.template" > "$CONF"
  # apt 套件把 JSON / Search / Bloom / TimeSeries 模組放在 /usr/lib/redis/modules，要 loadmodule 才會生效
  if ls /usr/lib/redis/modules/*.so >/dev/null 2>&1; then
    { echo; echo "# ---------- 模組（由 install-redis.sh 依 /usr/lib/redis/modules 自動加入）----------";
      for m in /usr/lib/redis/modules/*.so; do echo "loadmodule $m"; done; } >> "$CONF"
  fi
  [[ "$NO_SYSTEMD" == 0 ]] || sed -i 's/^supervised systemd/supervised no/' "$CONF"
  chown root:redis "$CONF"; chmod 640 "$CONF"   # 含密碼：只有 root 與 redis 可讀
fi
ok "bind=${BIND} port=${PORT} maxmemory=${MAXMEMORY} dir=${DATA_DIR}"

# ---------- 5. OS 調校 ----------
log "5/7 OS 調校"
if [[ "$SKIP_TUNING" == 1 ]]; then warn "--skip-tuning：略過"; else
  if [[ -d /etc/sysctl.d ]]; then
    run cp "$SCRIPT_DIR/99-redis-sysctl.conf" /etc/sysctl.d/99-redis.conf
    run sysctl --system >/dev/null 2>&1 || warn "sysctl --system 失敗（容器內屬正常）"
    ok "sysctl：vm.overcommit_memory=$(sysctl -n vm.overcommit_memory 2>/dev/null || echo '?') net.core.somaxconn=$(sysctl -n net.core.somaxconn 2>/dev/null || echo '?')"
  fi
  if [[ -f /sys/kernel/mm/transparent_hugepage/enabled ]]; then
    run bash -c 'echo never > /sys/kernel/mm/transparent_hugepage/enabled' 2>/dev/null || warn "無法寫入 THP 設定"
    if [[ "$NO_SYSTEMD" == 0 ]]; then
      run cp "$SCRIPT_DIR/disable-thp.service" /etc/systemd/system/disable-thp.service
      run systemctl daemon-reload; run systemctl enable --now disable-thp.service >/dev/null 2>&1 || true
    fi
    ok "THP：$(cat /sys/kernel/mm/transparent_hugepage/enabled)"
  else
    ok "此核心沒有 THP 介面，略過"
  fi
fi

# ---------- 6. systemd ----------
log "6/7 安裝並啟動服務"
if [[ "$NO_SYSTEMD" == 1 ]]; then
  if [[ "$DRY_RUN" == 0 ]]; then
    su -s /bin/bash redis -c "$PREFIX/bin/redis-server $CONF --daemonize yes"
    sleep 1
  fi
  ok "以 --daemonize 方式啟動（無 systemd 模式）"
else
  if [[ "$DRY_RUN" == 1 ]]; then echo "  [dry-run] render redis.service -> /etc/systemd/system/redis.service"; else
    render "$SCRIPT_DIR/redis.service" > /etc/systemd/system/redis.service
  fi
  run systemctl daemon-reload
  run systemctl enable --now redis.service
  ok "systemctl is-active redis：$(systemctl is-active redis 2>/dev/null || true)"
fi

# ---------- 7. 驗證 ----------
log "7/7 驗證"
[[ "$DRY_RUN" == 1 ]] && { echo "  [dry-run] 略過驗證"; exit 0; }
CLI=("$PREFIX/bin/redis-cli" -h "$([[ "$BIND" == 0.0.0.0 ]] && echo 127.0.0.1 || echo "${BIND%% *}")" -p "$PORT" -a "$PASSWORD" --no-auth-warning)
for i in $(seq 1 20); do "${CLI[@]}" ping >/dev/null 2>&1 && break; sleep 0.5; done
[[ "$("${CLI[@]}" ping)" == PONG ]] && ok "PING → PONG" || die "PING 失敗（journalctl -u redis 查日誌）"
"${CLI[@]}" set vm:install:check "$(date -Is)" >/dev/null && [[ -n "$("${CLI[@]}" get vm:install:check)" ]] && ok "SET/GET 正常"
"${CLI[@]}" del vm:install:check >/dev/null
ok "版本：$("${CLI[@]}" info server | awk -F: '/^redis_version/{print $2}' | tr -d '\r')"
ok "持久化：aof_enabled=$("${CLI[@]}" info persistence | awk -F: '/^aof_enabled/{print $2}' | tr -d '\r')  rdb_last_bgsave_status=$("${CLI[@]}" info persistence | awk -F: '/^rdb_last_bgsave_status/{print $2}' | tr -d '\r')"
ok "maxmemory=$("${CLI[@]}" config get maxmemory | tail -1)  policy=$("${CLI[@]}" config get maxmemory-policy | tail -1)"
if grep -qi "WARNING" "$LOG_DIR/redis.log" 2>/dev/null; then
  warn "啟動日誌有 WARNING（多半是 overcommit / THP，見 docs/03-deploy-vm.md）："
  grep -i "WARNING" "$LOG_DIR/redis.log" | tail -3 | sed 's/^/      /'
else
  ok "啟動日誌沒有 WARNING"
fi

cat <<SUMMARY

============================================================
 Redis 安裝完成
   連線：  ${PREFIX}/bin/redis-cli -h ${BIND%% *} -p ${PORT} -a '${PASSWORD}'
   設定：  ${CONF}
   資料：  ${DATA_DIR}     日誌：${LOG_DIR}/redis.log
   服務：  systemctl status redis   /   journalctl -u redis -f
   健檢：  scripts/health-check.sh -p ${PORT} -a '${PASSWORD}'
============================================================
SUMMARY
