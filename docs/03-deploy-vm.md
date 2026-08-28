# 03 · VM 佈署：從裸機到通過驗證

> 容器適合學習；到了正式環境，很多團隊的第一站仍是 VM。這一章提供**一鍵腳本**與**手動 runbook**，
> 在真實 VM（Fedora 44 / Linux 6.18，WSL2 on Hyper-V）與乾淨的 Ubuntu 24.04 上完整跑過（見 [第 9 節](#9-實測紀錄)）。

## 目錄

1. [VM 與容器的差別](#1-vm-與容器的差別)
2. [三條安裝路線](#2-三條安裝路線)
3. [一鍵佈署：install-redis.sh](#3-一鍵佈署install-redissh)
4. [手動 runbook](#4-手動-runbook)
5. [redis.conf 正式環境重點](#5-redisconf-正式環境重點)
6. [systemd 服務](#6-systemd-服務)
7. [OS 調校](#7-os-調校)
8. [多台 VM：主從與 Sentinel](#8-多台-vm主從與-sentinel)
9. [實測紀錄](#9-實測紀錄)
10. [故障排除](#10-故障排除)

檔案都在 `deploy/vm/`：

```
deploy/vm/
├── install-redis.sh        一鍵佈署（apt / dnf / source 自動選擇）
├── uninstall-redis.sh      還原
├── redis.conf.template     正式環境設定範本（腳本會填入 port / bind / 密碼 / maxmemory）
├── redis.service           systemd unit 範本
├── disable-thp.service     開機關閉 Transparent Huge Pages
├── 99-redis-sysctl.conf    核心參數
└── Vagrantfile             一鍵建立 Ubuntu 24.04 VM 練習
```

---

## 1. VM 與容器的差別

| 面向 | 容器 | VM |
|---|---|---|
| 行程管理 | Docker restart policy | **systemd**：graceful shutdown、開機自啟、資源限制、journal |
| OS 參數 | overcommit / THP 改不了（宿主機的） | 全部可控 |
| 資料 | volume | 直接在磁碟，生命週期跟機器一樣 |
| 安全邊界 | namespace | 整台機器，可用 systemd 沙箱（`ProtectSystem`、`ProtectHome`） |
| 效能 | 多一層網路（bridge）；`--network host` 可避免 | 原生 |

VM 特別要注意三件容器裡感受不到的事：
1. **記憶體要留給 fork**：`maxmemory` ≤ 實體記憶體 60–70%
2. **虛擬化層**：關掉 memory ballooning；注意 CPU steal time（`top` 的 `%st` > 5% 就該換宿主機）
3. **磁碟**：AOF 的 `everysec` fsync 在慢磁碟上會週期性卡住主執行緒；用 SSD

---

## 2. 三條安裝路線

| 路線 | 指令 | 版本 | 內建模組（JSON / Search / Bloom / TS） | 適用 |
|---|---|---|---|---|
| **apt**（Ubuntu / Debian） | `packages.redis.io` 官方倉庫 | 最新（8.10.1） | **有**（`/usr/lib/redis/modules/*.so`，腳本自動 `loadmodule`） | Ubuntu / Debian 的首選 |
| **dnf / yum**（RHEL / Rocky / Alma） | 發行版 AppStream | 舊（RHEL 9 是 6.2 / 7.x） | 無 | 只要核心、想跟發行版一起更新 |
| **source**（任何 Linux） | 下載 tarball 編譯 | 你指定 | 無（Redis 8.10 編譯模組需要 LLVM 21 + Rust + CMake，腳本只編核心 `make build redis`） | Fedora（只提供 Valkey）、需要特定版本、需要 TLS |

`install-redis.sh --method auto` 的選擇順序：有 `apt-get` → apt；有 `dnf` 且非 Fedora 且倉庫有 `redis` → dnf；否則 → source。

> 要模組又不能用 apt？用 `redis:8` 容器映像，或到 <https://github.com/redis/redis/releases> 抓官方預編譯的 tarball。

---

## 3. 一鍵佈署：install-redis.sh

```bash
cd deploy/vm

# 看它會做什麼（不改任何東西）
sudo DRY_RUN=1 ./install-redis.sh

# 全預設：自動選路線、bind 127.0.0.1、隨機密碼（結尾印出）、maxmemory 1gb
sudo ./install-redis.sh

# 正式環境常見的樣子
sudo ./install-redis.sh \
    --bind 10.0.1.15 \                 # 內網 IP；不要 0.0.0.0
    --password 'S3cure-P@ss' \
    --maxmemory 10gb \
    --data-dir /data/redis             # 獨立資料碟

# 強制原始碼編譯特定版本
sudo ./install-redis.sh --method source --version 8.10.1

# 容器 / 無 systemd 的環境（CI 測試用）
sudo ./install-redis.sh --no-systemd
```

腳本做七件事，每一件對應手動 runbook 的一步：

| 步驟 | 內容 | 失敗時 |
|---|---|---|
| 1 preflight | root 檢查、OS 偵測、**port 是否被占用**、選路線 | 直接停止，不會動系統 |
| 2 帳號目錄 | `redis` 系統帳號（nologin）、`/etc/redis` `/var/lib/redis` `/var/log/redis`（750） | |
| 3 安裝 | apt：加官方 GPG key 與倉庫；source：下載 + **SHA-256 驗證**（對照官方 redis-hashes）+ `make build redis` + `make install PREFIX=/opt/redis` | 編譯日誌在 `/tmp/redis-build.log` |
| 4 設定檔 | 由 `redis.conf.template` 渲染 `/etc/redis/redis.conf`，權限 640（含密碼）；既有檔案先備份；apt 路線自動加 `loadmodule` | |
| 5 OS 調校 | `sysctl.d/99-redis.conf`、THP=never（含開機生效的 `disable-thp.service`） | `--skip-tuning` 跳過 |
| 6 systemd | 渲染 `redis.service`、`enable --now` | `journalctl -u redis` |
| 7 驗證 | PING、SET/GET、版本、持久化狀態、maxmemory、**啟動日誌有沒有 WARNING** | |

結尾會印出連線指令與密碼。接著跑完整驗證：

```bash
sudo ../../scripts/verify-vm.sh -a 'S3cure-P@ss'      # 19 項檢查，含 graceful restart 不丟資料
../../scripts/health-check.sh -a 'S3cure-P@ss'
../../scripts/smoke-test.sh -a 'S3cure-P@ss'
```

還原：`sudo ./uninstall-redis.sh`（保留 `/var/lib/redis`）或 `--purge`。

---

## 4. 手動 runbook

想理解每一步，或環境不標準時照著做（以 Ubuntu 24.04 + apt 為例，其他路線的差異註明在旁）：

```bash
# 1. 前置：port 沒被占用、有 sudo
ss -tlnp | grep 6379 || echo "6379 free"

# 2. 帳號與目錄
sudo useradd --system --home-dir /var/lib/redis --shell /usr/sbin/nologin --user-group redis
sudo mkdir -p /etc/redis /var/lib/redis /var/log/redis
sudo chown redis:redis /var/lib/redis /var/log/redis && sudo chmod 750 /var/lib/redis /var/log/redis

# 3a. apt：官方倉庫
sudo apt-get install -y lsb-release curl gpg
curl -fsSL https://packages.redis.io/gpg | sudo gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main" \
  | sudo tee /etc/apt/sources.list.d/redis.list
sudo apt-get update && sudo apt-get install -y redis
sudo systemctl disable --now redis-server && sudo systemctl mask redis-server   # 用我們自己的 unit，避免兩個 unit 打架

# 3b. source（任何 Linux）
sudo apt-get install -y build-essential pkg-config libssl-dev libsystemd-dev   # RHEL: gcc make openssl-devel systemd-devel
cd /usr/local/src
sudo curl -fsSLO https://download.redis.io/releases/redis-8.10.1.tar.gz
curl -fsSL https://raw.githubusercontent.com/redis/redis-hashes/master/README | grep redis-8.10.1.tar.gz   # 對 SHA-256
sudo tar xzf redis-8.10.1.tar.gz && cd redis-8.10.1
sudo make -j"$(nproc)" build redis BUILD_TLS=yes USE_SYSTEMD=yes    # 只編核心；有 libsystemd 才能 Type=notify
sudo make install PREFIX=/opt/redis
for b in redis-server redis-cli redis-benchmark redis-check-aof redis-check-rdb; do sudo ln -sf /opt/redis/bin/$b /usr/local/bin/$b; done

# 4. 設定檔：用範本填值
sudo sed -e 's|__PORT__|6379|' -e 's|__BIND__|127.0.0.1|' -e 's|__PASSWORD__|S3cure-P@ss|' \
         -e 's|__MAXMEMORY__|1gb|' -e 's|__DATA_DIR__|/var/lib/redis|' -e 's|__LOG_DIR__|/var/log/redis|' \
         deploy/vm/redis.conf.template | sudo tee /etc/redis/redis.conf >/dev/null
sudo chown root:redis /etc/redis/redis.conf && sudo chmod 640 /etc/redis/redis.conf
# apt 路線：把模組加進去
ls /usr/lib/redis/modules/*.so 2>/dev/null | sed 's/^/loadmodule /' | sudo tee -a /etc/redis/redis.conf

# 5. OS 調校
sudo cp deploy/vm/99-redis-sysctl.conf /etc/sysctl.d/99-redis.conf && sudo sysctl --system
sudo cp deploy/vm/disable-thp.service /etc/systemd/system/ && sudo systemctl enable --now disable-thp

# 6. systemd（PREFIX：apt 是 /usr，source 是 /opt/redis）
sudo sed -e 's|__PREFIX__|/usr|' -e 's|__CONF__|/etc/redis/redis.conf|' \
         -e 's|__DATA_DIR__|/var/lib/redis|' -e 's|__LOG_DIR__|/var/log/redis|' \
         deploy/vm/redis.service | sudo tee /etc/systemd/system/redis.service >/dev/null
sudo systemctl daemon-reload && sudo systemctl enable --now redis
systemctl status redis --no-pager

# 7. 驗證
redis-cli -a 'S3cure-P@ss' --no-auth-warning ping
sudo grep -i warning /var/log/redis/redis.log || echo "no warnings"
```

---

## 5. redis.conf 正式環境重點

`deploy/vm/redis.conf.template` 每一段都有註解，這裡只列「跟練習環境不一樣、而且會出事」的：

| 設定 | 值 | 為什麼 |
|---|---|---|
| `bind` | 內網 IP（不是 0.0.0.0） | 無密碼綁公網的 Redis 幾分鐘內會被植入挖礦 |
| `protected-mode yes` + `requirepass` | | 兩道鎖；進一步用 ACL（[06](06-operations.md)） |
| `daemonize no` + `supervised systemd` | | 讓 systemd 管行程；`supervised systemd` 讓 Redis 載入完資料才通知「啟動完成」 |
| `maxmemory` | 實體記憶體 60–70% | 不設 = 等 OOM killer |
| `maxmemory-policy` | 快取 `allkeys-lru`；資料 `noeviction` | 見 [01 §4](01-architecture.md#4-過期與淘汰記憶體滿了怎麼辦) |
| `appendonly yes` + `appendfsync everysec` + `aof-use-rdb-preamble yes` | | 最多丟 1 秒 |
| `lazyfree-lazy-*` | yes | 刪大 key / 淘汰 / 過期 不卡主執行緒 |
| `client-output-buffer-limit replica 256mb 64mb 60` | | 全量同步時 Master 端的緩衝；太小會同步失敗無限重來 |
| `slowlog-log-slower-than 10000` + `latency-monitor-threshold 100` | | 事後追查用（[04](04-performance-tuning.md)） |
| `rename-command FLUSHALL ""` | 視需要 | 或用 ACL 的 `-@dangerous` |

線上改設定：`CONFIG SET maxmemory 2gb` 立即生效但重啟消失；`CONFIG REWRITE` 寫回檔案。

---

## 6. systemd 服務

`deploy/vm/redis.service` 的重點：

```ini
[Service]
Type=notify                  # 配合 redis.conf 的 supervised systemd：資料載入完成才算 active
User=redis                   # 絕不用 root 跑
ExecStart=/opt/redis/bin/redis-server /etc/redis/redis.conf
ExecStop=/bin/kill -s TERM $MAINPID    # SIGTERM = graceful：先寫 RDB/AOF 再退出
TimeoutStopSec=90            # 資料大時加大，不然 systemd 會 SIGKILL 造成資料丟失
Restart=on-failure
LimitNOFILE=65536            # maxclients 10000 需要
OOMScoreAdjust=-800          # OOM killer 先殺別人
# 沙箱
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full           # /usr /boot /etc 唯讀
ProtectHome=true
ReadWritePaths=/var/lib/redis /var/log/redis
RuntimeDirectory=redis       # /run/redis 放 pid
```

常用操作：

```bash
sudo systemctl status redis
sudo systemctl restart redis            # graceful；實測 214 ms 內恢復服務
sudo journalctl -u redis -f             # systemd 日誌
sudo tail -f /var/log/redis/redis.log   # Redis 自己的日誌
systemctl show redis -p NRestarts       # 非預期重啟次數（>0 要查）
```

---

## 7. OS 調校

Redis 啟動時會對三件事發 WARNING，`install-redis.sh` 全部處理掉了；驗證腳本會檢查日誌沒有 WARNING。

| 設定 | 值 | 沒設會怎樣 |
|---|---|---|
| `vm.overcommit_memory` | 1 | fork 做 bgsave 時 OS 可能拒絕配置記憶體 → `Can't save in background: fork: Cannot allocate memory` |
| Transparent Huge Pages | never | fork 後 copy-on-write 以 2 MB 為單位複製，延遲飆高、記憶體暴增 |
| `net.core.somaxconn` | ≥ 1024（配 `tcp-backlog`） | 突發連線被丟掉 |
| `vm.swappiness` | 1 | Redis 頁面進 swap = 延遲從 µs 變 ms |
| `ulimit -n` / `LimitNOFILE` | 65536 | `maxclients` 被迫縮小，連線被拒 |

```bash
# 檢查
sysctl vm.overcommit_memory net.core.somaxconn vm.swappiness
cat /sys/kernel/mm/transparent_hugepage/enabled     # always madvise [never]
sudo grep -i warning /var/log/redis/redis.log       # 應該是空的
```

防火牆（只開給應用伺服器）：

```bash
# firewalld（RHEL / Fedora）
sudo firewall-cmd --permanent --new-zone=redis
sudo firewall-cmd --permanent --zone=redis --add-source=10.0.1.0/24 --add-port=6379/tcp
sudo firewall-cmd --reload
# ufw（Ubuntu）
sudo ufw allow from 10.0.1.0/24 to any port 6379 proto tcp
```

---

## 8. 多台 VM：主從與 Sentinel

三台 VM（10.0.1.11 / 12 / 13），每台先跑 `install-redis.sh --bind <自己的 IP> --password 同一組`，然後：

```bash
# 12、13：指向 Master
redis-cli -h 10.0.1.12 -a 'pass' CONFIG SET masterauth 'pass'
redis-cli -h 10.0.1.12 -a 'pass' REPLICAOF 10.0.1.11 6379
redis-cli -h 10.0.1.12 -a 'pass' CONFIG REWRITE          # 寫回 redis.conf
# 11 也要有 masterauth：failover 後它會變 Replica
redis-cli -h 10.0.1.11 -a 'pass' CONFIG SET masterauth 'pass' && redis-cli -h 10.0.1.11 -a 'pass' CONFIG REWRITE
redis-cli -h 10.0.1.11 -a 'pass' INFO replication         # connected_slaves:2
```

Sentinel 每台一個（`/etc/redis/sentinel.conf`）：

```conf
port 26379
bind 10.0.1.11                       # 各自的 IP
sentinel monitor mymaster 10.0.1.11 6379 2
sentinel auth-pass mymaster pass
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
```

```bash
sudo cp deploy/vm/redis.service /etc/systemd/system/redis-sentinel.service   # 改 ExecStart 為 redis-server /etc/redis/sentinel.conf --sentinel
sudo systemctl enable --now redis-sentinel
redis-cli -h 10.0.1.11 -p 26379 SENTINEL get-master-addr-by-name mymaster
```

原理與故障轉移流程見 [01 §6](01-architecture.md#6-四種拓撲單機主從sentinelcluster)，容器版可先用 `docker-compose-sentinel.yml` 練習。

**Vagrant**（需要 VirtualBox）：`cd deploy/vm && vagrant up` 會建一台 Ubuntu 24.04 並自動跑 `install-redis.sh --method apt`，
宿主機用 `redis-cli -p 16379 -a redis-vm-pass ping` 連進去。

---

## 9. 實測紀錄

**環境 A：真實 VM**——WSL2（Hyper-V）上的 Fedora 44、Linux 6.18、16 vCPU / 15 GB、systemd 259，同機還跑著十幾個 Docker 容器（Kafka、ELK、Prometheus…），是一台「不乾淨的機器」。

```bash
sudo deploy/vm/install-redis.sh --password redis-vm-pass --maxmemory 512mb
```

| 項目 | 結果 |
|---|---|
| 路線 | Fedora 只有 Valkey → 自動選 **source**；SHA-256 對照官方雜湊通過 |
| 編譯 | `make -j16 build redis BUILD_TLS=yes USE_SYSTEMD=yes` **31 秒** |
| systemd | `Type=notify`、`User=redis`、`LimitNOFILE=65536`、`OOMScoreAdjust=-800` 全部生效 |
| OS | `vm.overcommit_memory=1`、`somaxconn=1024`、THP `[never]`；**redis.log 沒有 WARNING** |
| `verify-vm.sh` | **19 / 19 通過**；`systemctl restart redis` 後 **214 ms** 恢復服務、資料仍在 |
| `smoke-test.sh` | 28 / 28（9 種資料型別 + 鎖 + 限流） |
| `benchmark.sh` | SET 140k / GET 151k ops/s（無 pipeline）；pipeline 16：SET 952k / GET 1.43M；p50 0.079 ms |
| `backup.sh` | BGSAVE → 複製 → `redis-check-rdb` 通過 |

實測遇到的問題與處理（都已內建到腳本）：

| 問題 | 症狀 | 處理 |
|---|---|---|
| Redis 8.10 的 `make` 預設連模組一起編 | `redisearch / redisjson / redistimeseries: FAILED`（需要 LLVM 21 + Rust） | 改用 `make build redis` 只編核心；要模組請走 apt 或容器 |
| preflight 擋下 port 衝突 | 同機的 Sentinel 練習環境映射了 6379 | 先 `docker compose down`；腳本在動任何東西之前就停止 |
| `systemd-devel` 把 systemd 從 259.7 升到 259.8 | dnf 依賴解析 | 屬正常套件升級；不想動 systemd 可先裝 `systemd-devel` 或 `USE_SYSTEMD=no`（改用 `Type=simple`） |

**環境 B：乾淨的 Ubuntu 24.04**（`ubuntu:24.04` 容器，`--no-systemd`）

```bash
deploy/vm/install-redis.sh --method apt --no-systemd --password apt-test-pass
```

| 項目 | 結果 |
|---|---|
| 路線 | apt，`packages.redis.io` → `redis 6:8.10.1-1rl1~noble1` |
| 模組 | `/usr/lib/redis/modules/` 有 `rejson.so redisearch.so redisbloom.so redistimeseries.so`，腳本自動 `loadmodule` |
| 驗證 | 7 步全綠；PING / SET / AOF / maxmemory 正常 |

---

## 10. 故障排除

| 症狀 | 看哪裡 | 常見原因 |
|---|---|---|
| `systemctl start redis` 卡住後 timeout | `journalctl -u redis` | `Type=notify` 但 redis.conf 沒有 `supervised systemd`，或 binary 不是用 `USE_SYSTEMD=yes` 編的 → 改 `Type=simple` |
| `Permission denied` 開 log / 資料目錄 | `ls -l /var/lib/redis /var/log/redis` | 目錄 owner 不是 redis；`ProtectSystem` 擋住不在 `ReadWritePaths` 的路徑 |
| `Can't save in background: fork: Cannot allocate memory` | `sysctl vm.overcommit_memory` | 要設 1 |
| 啟動就退出 `Bind: Address already in use` | `ss -tlnp \| grep 6379` | port 被占 |
| `NOAUTH Authentication required` | | 要 `-a` 密碼；或 ACL 使用者 |
| `DENIED Redis is running in protected mode` | | 沒設密碼卻從非 loopback 連 |
| `MISCONF ... RDB snapshots ... unable to persist` | `df -h`、日誌 | 磁碟滿或目錄不可寫；緊急時 `CONFIG SET stop-writes-on-bgsave-error no` |
| 延遲突然變高 | `redis-cli --latency`、`latency doctor`、`INFO stats` 的 `latest_fork_usec` | fork、THP、swap、慢指令 → [04](04-performance-tuning.md) |
