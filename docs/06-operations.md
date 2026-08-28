# 06 · 維運與安全：上線後每天會用到的事

> 安全設定、監控、備份還原、日常操作、升級、錯誤訊息對照、上線檢查清單。
> 腳本：`scripts/health-check.sh`、`scripts/backup.sh`；容器 / VM 的差異在 [02](02-deploy-container.md) / [03](03-deploy-vm.md)。

## 目錄

1. [安全：五道防線](#1-安全五道防線)
2. [ACL：最小權限帳號](#2-acl最小權限帳號)
3. [TLS](#3-tls)
4. [監控與告警](#4-監控與告警)
5. [備份與還原](#5-備份與還原)
6. [日常操作](#6-日常操作)
7. [升級](#7-升級)
8. [錯誤訊息對照表](#8-錯誤訊息對照表)
9. [上線檢查清單](#9-上線檢查清單)

---

## 1. 安全：五道防線

沒有密碼、綁在 0.0.0.0 的 Redis，**幾分鐘內**就會被掃描器找到，用 `CONFIG SET dir /root/.ssh` + `SAVE` 寫入 SSH 金鑰，或直接植入挖礦程式。防線由外而內：

| 防線 | 做法 | 對應設定 |
|---|---|---|
| 1 網路 | 只有應用伺服器能連 6379 | 防火牆 / 安全群組；`bind 10.0.1.15`（內網 IP） |
| 2 protected-mode | 沒設密碼時拒絕非 loopback 連線 | `protected-mode yes`（預設） |
| 3 認證 | 密碼（32 字元以上，Redis 每秒可試上萬次） | `requirepass` → 進一步用 ACL |
| 4 授權 | 每個應用一個帳號，只給需要的指令與 key 前綴 | ACL（下一節） |
| 5 危險指令 | `FLUSHALL`、`CONFIG`、`DEBUG`、`KEYS` 不給應用帳號 | ACL `-@dangerous`；或 `rename-command` |

另外：
- **不要用 root 跑**（VM 腳本用 `redis` 帳號 + systemd 沙箱）
- 設定檔含密碼，權限 640
- 應用程式碼與日誌不要印密碼；連線字串放 secret manager

---

## 2. ACL：最小權限帳號

Redis 6+ 的 ACL 取代單一密碼（`requirepass` 其實就是 `default` 使用者的密碼）。

```bash
# 建立應用帳號：只能操作 order:* 與 cache:* 前綴、只能用讀寫類指令、不能用危險指令
ACL SETUSER app-order on >Str0ng-P@ss ~order:* ~cache:* +@read +@write +@hash +@list +@set +@sortedset +@stream -@dangerous
ACL SETUSER app-readonly on >ReadOnly-P@ss ~* +@read -@dangerous
ACL SETUSER monitoring on >Mon-P@ss ~* +info +ping +client|list +slowlog +latency +memory +config|get
ACL LIST
ACL WHOAMI
ACL CAT dangerous                     # 看 dangerous 類別有哪些（FLUSHALL、KEYS、CONFIG、DEBUG、SHUTDOWN…）
ACL SETUSER default off               # 最後把 default 關掉（確定所有應用都改用新帳號後！）
ACL SAVE                              # 寫到 aclfile（redis.conf 要設 aclfile /etc/redis/users.acl）

# 連線
redis-cli --user app-order --pass Str0ng-P@ss
AUTH app-order Str0ng-P@ss
```

規則語法：`on/off`、`>密碼`、`~key-pattern`、`+指令 / -指令 / +@類別 / -@類別`、`&channel-pattern`（Pub/Sub）、`allkeys / allcommands`。
`ACL LOG` 看被拒絕的存取。Module 13 的 `AclService` 有測試。

---

## 3. TLS

需要用 `BUILD_TLS=yes` 編譯的 binary（VM 腳本偵測到 OpenSSL 開發套件就會開；apt 套件與官方映像都支援）。

```bash
# 產生自簽憑證（正式環境用內部 CA）
openssl req -x509 -newkey rsa:4096 -nodes -keyout redis.key -out redis.crt -days 365 -subj "/CN=redis"
```

```conf
port 0                      # 關掉明文
tls-port 6379
tls-cert-file /etc/redis/tls/redis.crt
tls-key-file  /etc/redis/tls/redis.key
tls-ca-cert-file /etc/redis/tls/ca.crt
tls-auth-clients no         # yes = 要求 client 憑證（mTLS）
tls-replication yes         # 主從之間也加密
tls-cluster yes
```

```bash
redis-cli --tls --cacert ca.crt -h redis.internal ping
```

TLS 讓每個指令多約 10–20% CPU，且 pipeline 效益不變；跨機房或有合規要求時開。

---

## 4. 監控與告警

三層：

1. **能不能連**：`redis-cli ping`、exporter 的 `redis_up`
2. **健不健康**：`scripts/health-check.sh`（記憶體、持久化狀態、拒絕連線、slowlog、複寫）—— exit code 可接監控
3. **趨勢**：Prometheus + Grafana（`docker compose up -d` 內建儀表板），門檻見 [04 §8](04-performance-tuning.md#8-監控指標與告警門檻)

`INFO` 各區段最該看的欄位：

```bash
redis-cli info server        # redis_version、uptime_in_seconds、role
redis-cli info clients       # connected_clients、blocked_clients、maxclients
redis-cli info memory        # used_memory、maxmemory、mem_fragmentation_ratio、evicted 見 stats
redis-cli info persistence   # rdb_last_bgsave_status、aof_last_bgrewrite_status、rdb_changes_since_last_save、loading
redis-cli info stats         # instantaneous_ops_per_sec、rejected_connections、evicted_keys、keyspace_hits/misses、latest_fork_usec
redis-cli info replication   # role、connected_slaves、master_link_status、master_repl_offset
redis-cli info keyspace      # 每個 db 的 keys / expires
redis-cli info errorstats    # 每種錯誤的次數（Redis 6.2+）：errorstat_OOM、errorstat_NOAUTH…
```

最少要有的告警：Redis 掛了、記憶體 > 90%、`rdb_last_bgsave_status != ok`、Replica 斷線、`rejected_connections` 增加、slowlog 增加。

---

## 5. 備份與還原

### RDB 備份（`scripts/backup.sh`）

```bash
./scripts/backup.sh -a 密碼 -d /backup/redis        # BGSAVE → 等完成 → 複製 dump.rdb → redis-check-rdb 驗證 → 保留 7 份
# cron：每天 03:00
0 3 * * * /opt/redis-tutorial/scripts/backup.sh -a 密碼 -d /backup/redis >> /var/log/redis-backup.log 2>&1
```

RDB 檔可以直接複製（它是一次寫完 rename 的原子檔案）；遠端 Redis 用 `redis-cli --rdb backup.rdb` 透過複寫協定拉。
備份檔請送到**另一台機器 / 物件儲存**——本機磁碟壞了什麼都沒了。

### AOF

AOF 是目錄（`appendonlydir/`），要整個目錄一起備；備份時最好先 `BGREWRITEAOF` 讓它變小。
損壞修復：`redis-check-aof --fix appendonlydir/appendonly.aof.1.incr.aof`（會截掉損壞的尾巴）。

### 還原

```bash
sudo systemctl stop redis
sudo cp /backup/redis/dump-20260828.rdb /var/lib/redis/dump.rdb
sudo chown redis:redis /var/lib/redis/dump.rdb
# 開了 AOF 的話 Redis 會優先載入 AOF、忽略 RDB！要嘛先把 appendonlydir 移走，要嘛：
sudo mv /var/lib/redis/appendonlydir /var/lib/redis/appendonlydir.old
sudo systemctl start redis
redis-cli -a 密碼 dbsize             # 確認筆數
redis-cli -a 密碼 BGREWRITEAOF        # 從 RDB 內容重建 AOF
```

容器：`docker run --rm -v redis-tutorial_redis-data:/data -v /backup:/backup alpine cp /backup/dump.rdb /data/dump.rdb`。

### 演練

備份沒還原過等於沒備份。每季在測試機：還原 → `DBSIZE` → 抽查幾個 key → 記錄花了多久（這就是你的 RTO）。

---

## 6. 日常操作

```bash
# 設定
CONFIG GET maxmemory*                 # 看
CONFIG SET maxmemory 2gb              # 線上改，立即生效
CONFIG REWRITE                        # 寫回 redis.conf（否則重啟就沒了）

# 連線
CLIENT LIST                           # 誰連著、idle 多久、用什麼指令
CLIENT KILL ID 42 / CLIENT KILL ADDR 10.0.1.5:51234
CLIENT PAUSE 5000 WRITE               # 暫停寫入 5 秒（切換 Master 前用）

# 資料
DBSIZE
SCAN 0 MATCH 'session:*' COUNT 1000   # 不要 KEYS
OBJECT ENCODING key / MEMORY USAGE key / TTL key / TYPE key
UNLINK bigkey                         # 不要 DEL

# 持久化
BGSAVE / LASTSAVE / BGREWRITEAOF
INFO persistence

# 複寫
INFO replication
REPLICAOF NO ONE                      # 手動升為 Master
REPLICAOF 10.0.1.11 6379              # 指向新 Master
WAIT 1 1000                           # 等至少 1 個 Replica 確認寫入（最多 1 秒）

# 診斷
SLOWLOG GET 10 / LATENCY DOCTOR / MEMORY DOCTOR
DEBUG SLEEP 0                         # 不要在正式環境用 DEBUG
MONITOR                               # 只能短暫用，Ctrl-C

# 關機
SHUTDOWN SAVE                         # 存 RDB 再關；NOSAVE 直接關
```

**Cluster 日常**（在任一節點）：

```bash
redis-cli --cluster check 172.29.0.11:7001
redis-cli --cluster info 172.29.0.11:7001
redis-cli --cluster add-node 新節點:7007 172.29.0.11:7001            # 加 Master
redis-cli --cluster add-node 新節點:7008 172.29.0.11:7001 --cluster-slave   # 加 Replica
redis-cli --cluster rebalance 172.29.0.11:7001 --cluster-use-empty-masters # 把 slot 分給新 Master
redis-cli --cluster reshard 172.29.0.11:7001                            # 互動式搬 slot
redis-cli -p 7001 CLUSTER FAILOVER                                      # 在 Replica 上執行：手動切換（升級用）
```

---

## 7. 升級

Redis 保證 RDB / AOF **向後相容**（新版讀舊版檔案），反過來不保證——升級前先備份。

**單機**：備份 → 停 → 換 binary / 套件 / image tag → 啟 → `INFO server` 確認版本 → `DBSIZE` 確認資料。停機時間 = 載入 RDB 的時間（每 GB 約 10–20 秒）。

**主從 / Sentinel（零停機）**：
1. 先升 Replica（一台一台）
2. `CLIENT PAUSE 5000 WRITE` → 在 Sentinel 上 `SENTINEL FAILOVER mymaster` 讓某個已升級的 Replica 變 Master
3. 升舊 Master（現在是 Replica）

**Cluster**：每對主從照上面做，Replica 升完在它上面 `CLUSTER FAILOVER` 再升原 Master。

大版本（7 → 8）注意：預設值變動（`aof-use-rdb-preamble`、`save` 格式）、移除的指令；讀 release notes 的 "Breaking changes"。

---

## 8. 錯誤訊息對照表

| 錯誤 | 意思 | 處理 |
|---|---|---|
| `NOAUTH Authentication required` | 要密碼 | `AUTH` / `-a` / `--user --pass` |
| `WRONGPASS invalid username-password pair` | 帳密錯 | 檢查 ACL |
| `NOPERM this user has no permissions to run the 'x' command` | ACL 沒給這個指令 | `ACL SETUSER … +指令` |
| `DENIED Redis is running in protected mode` | 沒密碼從外部連 | 設密碼或 `protected-mode no`（只在內網） |
| `OOM command not allowed when used memory > 'maxmemory'` | 記憶體滿且 `noeviction` | 加記憶體 / 改策略 / 清資料 |
| `MISCONF Redis is configured to save RDB snapshots, but it's currently unable to persist to disk` | bgsave 失敗，拒寫保護 | 看日誌（磁碟滿 / 權限 / fork 失敗）；緊急 `CONFIG SET stop-writes-on-bgsave-error no` |
| `READONLY You can't write against a read only replica` | 寫到 Replica | 連 Master；Sentinel 環境問 Sentinel |
| `MOVED 5712 172.29.0.12:7002` | Cluster：key 在別的節點 | client 用 cluster 模式 / `redis-cli -c` |
| `ASK 5712 …` | Cluster：slot 搬移中 | 跟隨並先送 `ASKING`（cluster client 自動處理） |
| `CROSSSLOT Keys in request don't hash to the same slot` | Cluster 多 key 跨 slot | hash tag `{…}` |
| `CLUSTERDOWN The cluster is down` | slot 沒被覆蓋 | `--cluster check`；`cluster-require-full-coverage no` 可讓其他 slot 繼續服務 |
| `BUSY Redis is busy running a script` | Lua 跑太久 | `SCRIPT KILL`（沒寫入時）/ `SHUTDOWN NOSAVE`；改寫腳本 |
| `LOADING Redis is loading the dataset in memory` | 啟動中載入 RDB/AOF | 等；資料太大就拆 |
| `NOREPLICAS Not enough good replicas to write` | `min-replicas-to-write` 沒滿足 | 檢查 Replica 狀態 |
| `ERR max number of clients reached` | `maxclients` | 連線洩漏 / 加大 + `LimitNOFILE` |
| `WRONGTYPE Operation against a key holding the wrong kind of value` | 型別用錯 | `TYPE key` |
| `EXECABORT Transaction discarded because of previous errors` | MULTI 裡有語法錯 | 修指令 |
| `Can't save in background: fork: Cannot allocate memory` | overcommit | `vm.overcommit_memory=1` |
| Sentinel：`Can't resolve instance hostname` | 主機名稱解析 | 用 IP 或 `resolve-hostnames yes` |
| Sentinel：`+tilt` | 事件迴圈被卡（DNS / 時鐘跳動） | 用 IP；檢查 NTP |

---

## 9. 上線檢查清單

**安全**
- [ ] `bind` 內網 IP；防火牆只開給應用
- [ ] 密碼 ≥ 32 字元；應用用 ACL 帳號、`default` 關閉
- [ ] 危險指令不給應用帳號
- [ ] 非 root 執行；設定檔 640
- [ ] 跨網路傳輸有 TLS

**資料**
- [ ] `maxmemory` 已設，≤ 實體記憶體 70%；策略符合用途
- [ ] 持久化策略與 RPO 一致（everysec = 1 秒）
- [ ] 備份每天跑、送到異地、**還原演練過**
- [ ] 所有快取 key 有 TTL

**可用性**
- [ ] 單點？至少主從；不能人工介入就 Sentinel（3 個、不同機器）
- [ ] `min-replicas-to-write` 視資料重要性
- [ ] 應用端：連線池、逾時、重試、Sentinel / Cluster 感知的 client

**效能**
- [ ] [04 檢查清單](04-performance-tuning.md#9-檢查清單) 全部打勾
- [ ] OS：THP never、overcommit 1、日誌無 WARNING、`LimitNOFILE`

**監控**
- [ ] `health-check.sh` 或 exporter 接告警
- [ ] 告警：掛了、記憶體、bgsave 失敗、複寫斷線、拒絕連線、slowlog
- [ ] 儀表板有人看

**驗證**
- [ ] `scripts/verify-vm.sh`（VM）或 `scripts/verify-all.sh`（容器）通過
- [ ] `smoke-test.sh` 通過
- [ ] 在正式機型跑過 `benchmark.sh`，知道天花板
