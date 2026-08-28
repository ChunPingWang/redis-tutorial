# 03 · VM 佈署：從裸機到通過驗證

> **這一章要解決的問題**：容器環境（[02](02-deploy-container.md)）拿來學習很方便，但很多團隊的正式環境仍然是一台一台的 VM——
> 沒有 Docker、要自己管行程、自己調 OS、自己保證重開機後 Redis 會活著。這些事情在容器裡是 Docker 幫你做的，到了 VM 全部要自己來，
> 而且每一步做錯都有明確的後果（資料掉、fork 失敗、被 OOM killer 殺掉）。
>
> 所以這一章不只給指令，而是每一步先講「不做會怎樣」，再講「怎麼做」。提供**一鍵腳本**與**手動 runbook**，
> 全部在真實 VM（Fedora 44 / Linux 6.18，WSL2 on Hyper-V）與乾淨的 Ubuntu 24.04 上完整跑過（見 [第 9 節](#9-實測紀錄)）。

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

檔案都在 `deploy/vm/`。為什麼拆成這幾個檔案：腳本只負責「把範本填上你的參數、放到正確位置」，
真正決定行為的是範本本身——這樣你可以只看範本就知道正式環境長什麼樣，也能不跑腳本、手動複製範本自己來。

```
deploy/vm/
├── install-redis.sh        一鍵佈署（apt / dnf / source 自動選擇）
├── uninstall-redis.sh      還原（練習完把機器清乾淨）
├── redis.conf.template     正式環境設定範本（腳本會填入 port / bind / 密碼 / maxmemory）
├── redis.service           systemd unit 範本
├── disable-thp.service     開機關閉 Transparent Huge Pages
├── 99-redis-sysctl.conf    核心參數
└── Vagrantfile             一鍵建立 Ubuntu 24.04 VM 練習
```

---

## 1. VM 與容器的差別

**為什麼要先弄清楚差別**：如果你只在容器裡跑過 Redis，會以為「`docker run` 起來就完成了」——因為 Docker 默默幫你做了行程管理、
重啟、日誌收集，而宿主機的核心參數你根本碰不到。到了 VM，這些全變成你的責任；不知道差在哪，就不知道哪些步驟不能省。

| 面向 | 容器 | VM | 到了 VM 你要自己做的事 |
|---|---|---|---|
| 行程管理 | Docker restart policy | **systemd** | 寫 unit：開機自啟、掛了重啟、關機時先把資料寫完 |
| OS 參數 | overcommit / THP 改不了（宿主機的） | 全部可控 | 這是好事——容器裡那些 WARNING 在 VM 上終於能真正修掉 |
| 資料 | volume | 直接在磁碟 | 決定資料目錄放哪顆碟、誰有權限 |
| 安全邊界 | namespace | 整台機器 | 用專用帳號跑、systemd 沙箱限制 Redis 能碰的路徑 |
| 效能 | 多一層 bridge 網路 | 原生 | 沒有額外負擔，但也沒有人幫你限制它吃多少記憶體 |

**VM 特別要注意的三件事**——這三件在容器裡感受不到，卻是 VM 上最常出事的地方：

1. **記憶體要留給 fork**。Redis 做 RDB 快照或 AOF 重寫時會 fork 一個子行程，靠 copy-on-write 共用記憶體；寫入越多、複製的頁越多，極端下要兩倍記憶體。容器裡 cgroup limit 會直接把它殺掉（沒有日誌），VM 上則是整台機器開始 swap 或觸發 OOM killer。所以 `maxmemory` 只能設到實體記憶體的 60–70%。
2. **虛擬化層會偷走時間**。Redis 是單執行緒、延遲以微秒計；宿主機把 CPU 分給別的 VM（steal time）或做 memory ballooning，Redis 的延遲就會無法解釋地抖動。`top` 的 `%st` 持續 > 5% 就該換宿主機、關掉 ballooning。
3. **磁碟決定 AOF 的穩定性**。`appendfsync everysec` 每秒 fsync 一次；磁碟慢的時候 fsync 完不成，主執行緒最多會被擋 2 秒。VM 的虛擬磁碟常常比你想的慢，正式環境請用 SSD。

---

## 2. 三條安裝路線

**為什麼有三條**：不同 Linux 發行版拿到 Redis 的方式差很多——有的有官方最新版套件、有的只有幾年前的舊版、有的（Fedora）因為授權爭議乾脆只提供 Valkey 分支。
如果你的應用要用 JSON / Search 這些模組，路線的選擇會直接決定你拿不拿得到。

| 路線 | 怎麼裝 | 拿到的版本 | 內建模組（JSON / Search / Bloom / TS） | 為什麼選它 |
|---|---|---|---|---|
| **apt**（Ubuntu / Debian） | `packages.redis.io` 官方倉庫 | 最新（8.10.1） | **有**（`/usr/lib/redis/modules/*.so`，腳本自動 `loadmodule`） | 有官方倉庫就用官方倉庫：版本新、有模組、`apt upgrade` 就能升級 |
| **dnf / yum**（RHEL / Rocky / Alma） | 發行版 AppStream | 舊（RHEL 9 是 6.2 / 7.x） | 無 | 只要核心功能、想跟發行版一起更新、公司政策不允許外部倉庫 |
| **source**（任何 Linux） | 下載 tarball 編譯 | 你指定 | 無（見下） | 發行版沒有 Redis（Fedora 只提供 Valkey）、需要特定版本、需要 TLS 或自訂編譯選項 |

> **為什麼原始碼路線沒有模組**：Redis 8.10 起 `make` 預設會連同 JSON / Search / TimeSeries 一起編譯，
> 但那需要 LLVM 21 + Rust + 特定版本的 CMake——一般 VM 上沒有，實測直接失敗。腳本因此改用 `make build redis` 只編核心。
> 要模組又不能用 apt？用 `redis:8` 容器映像，或到 <https://github.com/redis/redis/releases> 抓官方預編譯的 tarball。

`install-redis.sh --method auto` 的選擇順序就是上表的優先序：有 `apt-get` → apt；有 `dnf` 且非 Fedora 且倉庫有 `redis` → dnf；否則 → source。

---

## 3. 一鍵佈署：install-redis.sh

**為什麼要有一鍵腳本**：手動 runbook 有七個步驟、二十多個指令，每次裝一台就重打一遍，漏一步（例如忘了關 THP）不會馬上出錯，
而是三個月後在流量高峰 fork 卡住才發現。把步驟固化成腳本，每台機器的結果一致，而且腳本最後會自己驗證。

**怎麼用**：先 dry-run 看它會動哪些東西（它會改系統設定、建帳號、裝套件），確認後再真的跑。

```bash
cd deploy/vm

# 看它會做什麼（不改任何東西）
sudo DRY_RUN=1 ./install-redis.sh

# 全預設：自動選路線、bind 127.0.0.1、隨機密碼（結尾印出）、maxmemory 1gb
sudo ./install-redis.sh

# 正式環境常見的樣子
sudo ./install-redis.sh \
    --bind 10.0.1.15 \                 # 內網 IP；不要 0.0.0.0（見第 5 節「為什麼不能綁 0.0.0.0」）
    --password 'S3cure-P@ss' \
    --maxmemory 10gb \                 # 實體記憶體的 60–70%（第 1 節「留給 fork」）
    --data-dir /data/redis             # 獨立資料碟：不跟 OS 碟搶 I/O，磁碟滿了也不會拖垮系統

# 強制原始碼編譯特定版本
sudo ./install-redis.sh --method source --version 8.10.1

# 容器 / 無 systemd 的環境（CI 測試用）
sudo ./install-redis.sh --no-systemd
```

**它做的七件事**，每一件對應手動 runbook 的一步。為什麼順序是這樣：先檢查（不動系統）→ 準備身分與目錄 → 裝軟體 → 給設定 → 調 OS → 交給 systemd → 驗證；任何一步失敗都停下來，不會留下半套。

| 步驟 | 內容 | 為什麼需要這一步 | 失敗時 |
|---|---|---|---|
| 1 preflight | root 檢查、OS 偵測、**port 是否被占用**、選路線 | port 被占的話 Redis 會起不來但 systemd 會不斷重試，先擋下來最省事 | 直接停止，不會動系統 |
| 2 帳號目錄 | `redis` 系統帳號（nologin）、`/etc/redis` `/var/lib/redis` `/var/log/redis`（750） | 用 root 跑 Redis，任何一個 `CONFIG SET dir` 漏洞就是整台機器淪陷 | |
| 3 安裝 | apt：加官方 GPG key 與倉庫；source：下載 + **SHA-256 驗證**（對照官方 redis-hashes）+ `make build redis` + `make install PREFIX=/opt/redis` | 驗雜湊是因為你正在把一個會開網路 port 的二進位放上正式機器 | 編譯日誌在 `/tmp/redis-build.log` |
| 4 設定檔 | 由 `redis.conf.template` 渲染 `/etc/redis/redis.conf`，權限 640；既有檔案先備份；apt 路線自動加 `loadmodule` | 設定檔含密碼，不能讓其他使用者讀；備份是為了你重跑腳本時不會弄丟手動改過的設定 | |
| 5 OS 調校 | `sysctl.d/99-redis.conf`、THP=never（含開機生效的 `disable-thp.service`） | 見第 7 節：這三個設定沒做，Redis 啟動日誌會直接警告 | `--skip-tuning` 跳過 |
| 6 systemd | 渲染 `redis.service`、`enable --now` | 沒有 systemd，機器重開 Redis 就不見了；掛掉也沒人拉起來 | `journalctl -u redis` |
| 7 驗證 | PING、SET/GET、版本、持久化狀態、maxmemory、**啟動日誌有沒有 WARNING** | 「裝完了」和「裝對了」是兩回事；WARNING 代表第 5 步沒真正生效 | |

結尾會印出連線指令與密碼。**為什麼還要再跑一次完整驗證**：腳本內建的驗證只確認「Redis 活著且設定生效」，
`verify-vm.sh` 進一步檢查 systemd 的每個硬化選項、檔案權限、OS 參數，並實際 `systemctl restart` 一次確認資料不會丟——這才是正式環境要的保證。

```bash
sudo ../../scripts/verify-vm.sh -a 'S3cure-P@ss'      # 19 項檢查，含 graceful restart 不丟資料
../../scripts/health-check.sh -a 'S3cure-P@ss'
../../scripts/smoke-test.sh -a 'S3cure-P@ss'
```

還原：`sudo ./uninstall-redis.sh`（保留 `/var/lib/redis`）或 `--purge`。

---

## 4. 手動 runbook

**為什麼要看手動版**：腳本讓你「做對」，runbook 讓你「懂為什麼」。環境不標準（自訂路徑、公司內部倉庫、沒有網路）時，你需要知道每一步在做什麼才能改。
以下以 Ubuntu 24.04 + apt 為例，其他路線的差異註明在旁。每一步先講不做會怎樣。

**步驟 1：前置檢查。** 6379 被占用時 Redis 起不來；systemd 會依 `Restart=on-failure` 每 5 秒重試一次，日誌被塞滿卻不知道原因。

```bash
ss -tlnp | grep 6379 || echo "6379 free"
```

**步驟 2：專用帳號與目錄。** Redis 有 `CONFIG SET dir` + `SAVE` 可以把檔案寫到任意路徑——用 root 跑，攻擊者拿到 Redis 就等於拿到整台機器（寫 `/root/.ssh/authorized_keys` 是經典手法）。
所以要一個不能登入的系統帳號，資料與日誌目錄只有它能寫（750：其他人連列出都不行，因為 RDB 檔就是你的全部資料）。

```bash
sudo useradd --system --home-dir /var/lib/redis --shell /usr/sbin/nologin --user-group redis
sudo mkdir -p /etc/redis /var/lib/redis /var/log/redis
sudo chown redis:redis /var/lib/redis /var/log/redis && sudo chmod 750 /var/lib/redis /var/log/redis
```

**步驟 3a：apt 安裝。** 為什麼不用 Ubuntu 自帶的 `redis-server` 套件：版本舊（24.04 是 7.0）、沒有模組。官方倉庫要先匯入 GPG key，否則 apt 會拒絕（或你會裝到被竄改的套件）。
裝完套件自帶一個 `redis-server.service`，跟我們自己的 `redis.service` 會同時搶 6379——所以停掉並 mask 它，只留一個 unit 管事。

```bash
sudo apt-get install -y lsb-release curl gpg
curl -fsSL https://packages.redis.io/gpg | sudo gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main" \
  | sudo tee /etc/apt/sources.list.d/redis.list
sudo apt-get update && sudo apt-get install -y redis
sudo systemctl disable --now redis-server && sudo systemctl mask redis-server   # 用我們自己的 unit，避免兩個 unit 打架
```

**步驟 3b：原始碼安裝。** 為什麼要對 SHA-256：你從網路下載了一個會監聽網路 port、以系統服務身分常駐的程式，下載途中被替換你不會察覺。
為什麼 `make build redis` 而不是 `make`：8.10 的 `make` 會連模組一起編，需要 LLVM 21 + Rust，一般 VM 沒有。
`BUILD_TLS=yes` 讓之後能開 TLS（第 6 章）；`USE_SYSTEMD=yes` 讓 Redis 能用 `sd_notify` 告訴 systemd「我載入完資料了」（第 6 節 `Type=notify` 的前提）。
裝到 `/opt/redis` 而不是 `/usr/local`：跟系統套件隔開，升級或移除只要換一個目錄。

```bash
sudo apt-get install -y build-essential pkg-config libssl-dev libsystemd-dev   # RHEL: gcc make openssl-devel systemd-devel
cd /usr/local/src
sudo curl -fsSLO https://download.redis.io/releases/redis-8.10.1.tar.gz
curl -fsSL https://raw.githubusercontent.com/redis/redis-hashes/master/README | grep redis-8.10.1.tar.gz   # 對 SHA-256
sudo tar xzf redis-8.10.1.tar.gz && cd redis-8.10.1
sudo make -j"$(nproc)" build redis BUILD_TLS=yes USE_SYSTEMD=yes    # 只編核心；有 libsystemd 才能 Type=notify
sudo make install PREFIX=/opt/redis
for b in redis-server redis-cli redis-benchmark redis-check-aof redis-check-rdb; do sudo ln -sf /opt/redis/bin/$b /usr/local/bin/$b; done
```

**步驟 4：設定檔。** 為什麼用範本填值而不是拿套件自帶的 `redis.conf` 改：自帶的設定檔有一千多行，預設值是給「本機開發」用的（沒密碼、沒 maxmemory、沒 AOF），
你要改的地方散在各處，漏改一個就是正式環境事故。範本只留正式環境需要的設定，每一項都有註解（第 5 節）。
權限 640 + owner `root:redis`：設定檔裡有密碼，只有 root 能改、只有 redis 能讀。
apt 路線的模組 `.so` 檔裝在 `/usr/lib/redis/modules`，但不 `loadmodule` 就等於沒裝——`MODULE LIST` 會只有內建的 vectorset。

```bash
sudo sed -e 's|__PORT__|6379|' -e 's|__BIND__|127.0.0.1|' -e 's|__PASSWORD__|S3cure-P@ss|' \
         -e 's|__MAXMEMORY__|1gb|' -e 's|__DATA_DIR__|/var/lib/redis|' -e 's|__LOG_DIR__|/var/log/redis|' \
         deploy/vm/redis.conf.template | sudo tee /etc/redis/redis.conf >/dev/null
sudo chown root:redis /etc/redis/redis.conf && sudo chmod 640 /etc/redis/redis.conf
# apt 路線：把模組加進去
ls /usr/lib/redis/modules/*.so 2>/dev/null | sed 's/^/loadmodule /' | sudo tee -a /etc/redis/redis.conf
```

**步驟 5：OS 調校。** 不做的話 Redis 啟動時會印三個 WARNING，而它們不是「建議」——overcommit 沒開，資料大到某個程度 bgsave 就會 fork 失敗；THP 沒關，fork 期間延遲會飆。
為什麼 THP 要用一個 systemd unit 而不是寫一次就好：`/sys/kernel/mm/transparent_hugepage/enabled` 是記憶體裡的設定，重開機就回到 `always`，
所以要一個 `Before=redis.service` 的 oneshot unit 在每次開機、Redis 啟動前把它關掉（細節見第 7 節）。

```bash
sudo cp deploy/vm/99-redis-sysctl.conf /etc/sysctl.d/99-redis.conf && sudo sysctl --system
sudo cp deploy/vm/disable-thp.service /etc/systemd/system/ && sudo systemctl enable --now disable-thp
```

**步驟 6：systemd。** 這一步之前 Redis 只是「一個可以執行的程式」；這一步之後它才是「服務」：開機自啟、掛了自動拉起、關機時先把資料寫完。
`PREFIX` 依路線不同（apt 裝在 `/usr`、source 在 `/opt/redis`），填錯 `ExecStart` 就找不到執行檔。每一個選項的理由在第 6 節。

```bash
sudo sed -e 's|__PREFIX__|/usr|' -e 's|__CONF__|/etc/redis/redis.conf|' \
         -e 's|__DATA_DIR__|/var/lib/redis|' -e 's|__LOG_DIR__|/var/log/redis|' \
         deploy/vm/redis.service | sudo tee /etc/systemd/system/redis.service >/dev/null
sudo systemctl daemon-reload && sudo systemctl enable --now redis
systemctl status redis --no-pager
```

**步驟 7：驗證。** `PONG` 只證明行程活著；`grep WARNING` 才證明第 5 步真的生效——這是最常被跳過、也最常在幾個月後出事的一步。

```bash
redis-cli -a 'S3cure-P@ss' --no-auth-warning ping
sudo grep -i warning /var/log/redis/redis.log || echo "no warnings"
```

---

## 5. redis.conf 正式環境重點

**為什麼這一節只列幾項**：`deploy/vm/redis.conf.template` 每一段都有註解，這裡只挑「跟練習環境不一樣、而且設錯會出事」的。
練習環境（`docker/redis/redis.conf`）為了方便沒密碼、綁 0.0.0.0——那些設定搬到正式環境就是資安事故。

| 設定 | 值 | 不這樣設會怎樣 | 它怎麼解決 |
|---|---|---|---|
| `bind` | 內網 IP（不是 0.0.0.0） | 綁 0.0.0.0 + 沒密碼的 Redis，掃描器幾分鐘內就找到，直接植入挖礦程式 | 只在應用能到達的介面監聽；配合防火牆是兩道鎖 |
| `protected-mode yes` + `requirepass` | | Redis 每秒能試幾萬個密碼，弱密碼等於沒密碼 | 32 字元以上的密碼；進一步用 ACL 給每個應用最小權限（[06](06-operations.md)） |
| `daemonize no` + `supervised systemd` | | `daemonize yes` 會讓 Redis 自己 fork 到背景，systemd 追蹤不到主行程，以為它掛了又拉一個 | 前景執行交給 systemd 管；`supervised systemd` 讓 Redis 載入完資料才通知「啟動完成」，否則 systemd 在資料還在載入時就宣告 active |
| `maxmemory` | 實體記憶體 60–70% | 不設 = 沒有上限，用到整台機器記憶體耗盡，被 OOM killer 殺掉且連淘汰的機會都沒有 | 到了上限依策略淘汰或拒寫，留 30–40% 給 fork 與 OS |
| `maxmemory-policy` | 快取 `allkeys-lru`；資料 `noeviction` | 預設 `noeviction`：純快取用途下記憶體滿了寫入全部失敗，應用炸掉 | 快取讓 Redis 自己淘汰舊資料；不能丟的資料則寧可拒寫並靠監控告警（[01 §4](01-architecture.md#4-過期與淘汰記憶體滿了怎麼辦)） |
| `appendonly yes` + `appendfsync everysec` + `aof-use-rdb-preamble yes` | | 只靠 RDB 快照，重啟會丟最後一次快照之後的所有寫入（可能是幾分鐘） | AOF 把每個寫入追加到檔案，最多丟 1 秒；混合模式讓檔案小、重啟載入快 |
| `lazyfree-lazy-*` | yes | 刪除一個百萬元素的 key、或淘汰大 key 時，主執行緒同步釋放記憶體，所有 client 一起卡住 | 釋放交給背景執行緒 |
| `client-output-buffer-limit replica 256mb 64mb 60` | | Replica 全量同步期間，Master 要暫存這段時間的寫入；緩衝太小就被切斷、重來、再切斷，永遠同步不完 | 給 Replica 足夠的緩衝 |
| `slowlog-log-slower-than 10000` + `latency-monitor-threshold 100` | | 出事後沒有任何紀錄，不知道是哪個指令慢 | 事後追查用（[04](04-performance-tuning.md)），幾乎沒有成本 |
| `rename-command FLUSHALL ""` | 視需要 | 應用程式一個 bug 或一次誤操作就清空整個資料庫 | 讓這些指令不存在；或用 ACL 的 `-@dangerous` 只對應用帳號封鎖 |

線上改設定：`CONFIG SET maxmemory 2gb` 立即生效但重啟消失；記得 `CONFIG REWRITE` 寫回檔案，不然下次重啟又回到舊值——這是很常見的「明明改過怎麼又變回去」。

---

## 6. systemd 服務

**為什麼不用 `nohup redis-server &`**：那樣機器重開 Redis 就不見了、掛了沒人拉、關機時直接被 SIGKILL 可能丟掉還沒寫入磁碟的資料、也沒有資源限制與沙箱。
systemd 一次解決這些，代價是要看懂一個 unit 檔。`deploy/vm/redis.service` 的每一行都有理由
（下面的行尾 `# 為什麼` 是說明用；**systemd 不支援行尾註解**，實際檔案的註解都在獨立行，請以 `deploy/vm/redis.service` 為準）：

```ini
[Service]
Type=notify                  # 為什麼：Redis 啟動後要花時間載入 RDB/AOF（每 GB 約 10–20 秒），Type=simple 會在資料還沒載完就宣告 active，
                             #         依賴它的服務跟著啟動然後連線失敗。notify 配合 redis.conf 的 supervised systemd，載入完才算啟動成功。
User=redis                   # 為什麼：見第 4 節步驟 2——root 跑 Redis 等於把整台機器交給任何拿到 Redis 的人
ExecStart=/opt/redis/bin/redis-server /etc/redis/redis.conf
ExecStop=/bin/kill -s TERM $MAINPID    # 為什麼：SIGTERM 讓 Redis 做 graceful shutdown（先寫 RDB/AOF 再退出）；
                                       #         不設的話 systemd 預設也是 SIGTERM，但寫明白比較不會被改壞
TimeoutStopSec=90            # 為什麼：資料大時寫 RDB 要幾十秒，超過這個時間 systemd 會 SIGKILL，資料就丟了；資料越大要設越長
Restart=on-failure           # 為什麼：Redis 崩潰自動拉起；但正常 SHUTDOWN 不拉（用 always 會讓你關不掉它）
LimitNOFILE=65536            # 為什麼：每個連線一個 file descriptor，預設 1024 會讓 maxclients 10000 被迫縮小，連線被拒
OOMScoreAdjust=-800          # 為什麼：記憶體真的不夠時，讓 OOM killer 先殺別的行程——Redis 一死資料全沒
# 沙箱：Redis 只需要讀寫兩個目錄，其他地方都不該碰
NoNewPrivileges=true         # 為什麼：即使 Redis 被攻破也拿不到更高權限
PrivateTmp=true              # 為什麼：獨立的 /tmp，避免 symlink 攻擊
ProtectSystem=full           # 為什麼：/usr /boot /etc 唯讀——攻擊者改不了系統檔
ProtectHome=true             # 為什麼：/home /root 看不到——經典的「寫 SSH 金鑰」攻擊直接失效
ReadWritePaths=/var/lib/redis /var/log/redis   # 為什麼：ProtectSystem 之後只有明列的路徑能寫
RuntimeDirectory=redis       # 為什麼：/run/redis 放 pid，開機時 systemd 幫你建好並給 redis 帳號
```

**怎麼日常操作**：這幾個指令是你每天會用的。`restart` 之所以能在 214 ms 內恢復（實測），是因為 graceful shutdown + AOF 混合模式載入快；
`NRestarts` 是「非預期重啟」的次數，> 0 代表 Redis 曾經崩潰過而你可能不知道。

```bash
sudo systemctl status redis
sudo systemctl restart redis            # graceful；實測 214 ms 內恢復服務
sudo journalctl -u redis -f             # systemd 日誌（啟動失敗的原因在這）
sudo tail -f /var/log/redis/redis.log   # Redis 自己的日誌（WARNING、bgsave、複寫狀態在這）
systemctl show redis -p NRestarts       # 非預期重啟次數（>0 要查）
```

---

## 7. OS 調校

**為什麼 Redis 對 OS 這麼敏感**：它是單執行緒、所有資料在記憶體、靠 fork 做持久化。這三個特性讓它對三件 OS 層的事特別脆弱：
記憶體配置策略（fork 能不能成功）、記憶體頁的大小（fork 之後複製多少）、以及有沒有被 swap 出去（微秒變毫秒）。
Redis 啟動時會對這些發 WARNING——`install-redis.sh` 全部處理掉了，驗證腳本會檢查日誌沒有 WARNING。

| 設定 | 值 | 沒設會怎樣 | 為什麼這個值有效 |
|---|---|---|---|
| `vm.overcommit_memory` | 1 | fork 時 OS 以為子行程要「再一份」同樣大的記憶體，機器沒那麼多就拒絕 → `Can't save in background: fork: Cannot allocate memory`，從此沒有快照 | 1 = 允許超額配置；實際上 copy-on-write 只複製被改到的頁，真正用不到那麼多 |
| Transparent Huge Pages | never | THP 把記憶體頁從 4 KB 變 2 MB；fork 後每次寫入要複製 2 MB 而不是 4 KB → 延遲飆高 500 倍、記憶體暴增 | 關掉就回到 4 KB 頁 |
| `net.core.somaxconn` | ≥ 1024（配 `tcp-backlog`） | 監聽佇列預設 128，突發連線（應用重啟、連線池同時建立）超過就被丟掉，應用看到 connection refused | Redis 的 `tcp-backlog 1024` 會被 OS 的 somaxconn 截斷，兩邊都要調 |
| `vm.swappiness` | 1 | 預設 60，OS 會「積極」把不常用的記憶體頁換到磁碟；Redis 的某個 key 被 swap 出去，讀它從 µs 變 ms，而你無法預測是哪個 | 1 = 幾乎不主動 swap，只在真的沒記憶體時才用 |
| `ulimit -n` / `LimitNOFILE` | 65536 | 每個連線一個 fd，預設 1024 → `maxclients` 被 Redis 自動縮小，多出來的連線被拒 | systemd 的 `LimitNOFILE` 已設（第 6 節） |

**怎麼確認生效**：sysctl 立即生效，但 THP 是每次開機都要關（第 4 節步驟 5 解釋了為什麼要用 unit）。最後一行 `grep WARNING` 是判斷標準——Redis 自己會告訴你哪一項沒做好。

```bash
sysctl vm.overcommit_memory net.core.somaxconn vm.swappiness
cat /sys/kernel/mm/transparent_hugepage/enabled     # always madvise [never]
sudo grep -i warning /var/log/redis/redis.log       # 應該是空的
```

**防火牆**：`bind` 內網 IP 是第一道鎖，但同一個內網的任何機器都還連得到；防火牆把範圍縮到「只有應用伺服器那個網段」，才是兩道鎖。

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

**為什麼一台不夠**：單機 Redis 是單點——機器一掛，快取全沒、Session 全掉、等你人工修好。主從讓你有一份即時副本（還能分擔讀取），
Sentinel 讓「Master 掛了換 Replica 上」這件事自動發生，不用半夜爬起來。原理與故障轉移流程見 [01 §6](01-architecture.md#6-四種拓撲單機主從sentinelcluster)，
容器版可先用 `docker-compose-sentinel.yml` 練習（[02 §3](02-deploy-container.md#3-sentinel-環境docker-compose-sentinelyml)）。

**怎麼做**：三台 VM（10.0.1.11 / 12 / 13），每台先跑 `install-redis.sh --bind <自己的 IP> --password 同一組`——
為什麼密碼要同一組：故障轉移後任何一台都可能變 Master、任何一台都可能變 Replica，設定必須「對稱」。
`masterauth` 是「我連上 Master 時要出示的密碼」，`requirepass` 是「別人連我要出示的密碼」；三台都要兩個一起設，不然切換後新的 Replica 連不上新的 Master。

```bash
# 12、13：指向 Master
redis-cli -h 10.0.1.12 -a 'pass' CONFIG SET masterauth 'pass'
redis-cli -h 10.0.1.12 -a 'pass' REPLICAOF 10.0.1.11 6379
redis-cli -h 10.0.1.12 -a 'pass' CONFIG REWRITE          # 寫回 redis.conf，否則重啟後忘記自己是 Replica
# 11 也要有 masterauth：failover 後它會變 Replica
redis-cli -h 10.0.1.11 -a 'pass' CONFIG SET masterauth 'pass' && redis-cli -h 10.0.1.11 -a 'pass' CONFIG REWRITE
redis-cli -h 10.0.1.11 -a 'pass' INFO replication         # connected_slaves:2
```

**Sentinel 每台一個**——為什麼是三個、為什麼放在三台不同機器：Sentinel 之間靠投票決定「Master 真的掛了」與「誰來主持切換」，
需要過半數同意；兩個 Sentinel 一個掛了就沒有過半數，三個才能容忍一個故障。放在同一台機器上，那台機器一掛三個一起掛，等於沒做。
`quorum 2`：至少兩個 Sentinel 都連不上 Master 才判定客觀下線，避免某一台 Sentinel 自己網路壞了就誤觸發切換。

```conf
port 26379
bind 10.0.1.11                       # 各自的 IP
sentinel monitor mymaster 10.0.1.11 6379 2
sentinel auth-pass mymaster pass     # Sentinel 也要密碼才能 INFO Master
sentinel down-after-milliseconds mymaster 5000    # 5 秒沒回應算主觀下線：太短會因為網路抖動誤判，太長切換慢
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1   # 切換後一次只讓一台 Replica 重新同步，其他台繼續服務讀取
```

Sentinel 本質上就是用 `--sentinel` 模式啟動的 redis-server，所以 unit 檔可以直接複用：

```bash
sudo cp deploy/vm/redis.service /etc/systemd/system/redis-sentinel.service   # 改 ExecStart 為 redis-server /etc/redis/sentinel.conf --sentinel
sudo systemctl enable --now redis-sentinel
redis-cli -h 10.0.1.11 -p 26379 SENTINEL get-master-addr-by-name mymaster
```

**Vagrant**：想在自己電腦上練 VM 佈署、又不想弄髒主機——`cd deploy/vm && vagrant up` 會建一台 Ubuntu 24.04（需要 VirtualBox）並自動跑 `install-redis.sh --method apt`，
宿主機用 `redis-cli -p 16379 -a redis-vm-pass ping` 連進去；`vagrant destroy -f` 整台丟掉。

---

## 9. 實測紀錄

**為什麼要留這一節**：文件說「腳本可以用」和「腳本在這台機器、這一天、跑出這些數字」是兩回事。
這裡記的是真實執行結果與踩到的問題，讓你知道哪些事是驗證過的、哪些坑已經被腳本吃掉了。

**環境 A：真實 VM**——WSL2（Hyper-V）上的 Fedora 44、Linux 6.18、16 vCPU / 15 GB、systemd 259，同機還跑著十幾個 Docker 容器（Kafka、ELK、Prometheus…），是一台「不乾淨的機器」——正好測出真實世界會遇到的 port 衝突與套件相依問題。

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

實測遇到的問題與處理——每一個都不是 Redis 的問題，而是「機器不是為 Redis 準備的」，這正是 preflight 放在第一步的原因：

| 問題 | 症狀 | 處理 |
|---|---|---|
| Redis 8.10 的 `make` 預設連模組一起編 | `redisearch / redisjson / redistimeseries: FAILED`（需要 LLVM 21 + Rust） | 改用 `make build redis` 只編核心；要模組請走 apt 或容器 |
| preflight 擋下 port 衝突 | 同機的 Sentinel 練習環境映射了 6379 | 先 `docker compose down`；腳本在動任何東西之前就停止 |
| `systemd-devel` 把 systemd 從 259.7 升到 259.8 | dnf 依賴解析 | 屬正常套件升級；不想動 systemd 可先裝 `systemd-devel` 或 `USE_SYSTEMD=no`（改用 `Type=simple`） |

**環境 B：乾淨的 Ubuntu 24.04**（`ubuntu:24.04` 容器，`--no-systemd`）——驗證 apt 路線在「什麼都沒裝」的機器上能不能從零跑完。

```bash
deploy/vm/install-redis.sh --method apt --no-systemd --password apt-test-pass
```

| 項目 | 結果 |
|---|---|
| 路線 | apt，`packages.redis.io` → `redis 6:8.10.1-1rl1~noble1` |
| 模組 | `/usr/lib/redis/modules/` 有 `rejson.so redisearch.so redisbloom.so redistimeseries.so`，腳本自動 `loadmodule`；`MODULE LIST` 看到 JSON / Search / Bloom / TimeSeries / VectorSet |
| 驗證 | 7 步全綠；PING / SET / AOF / maxmemory 正常；`smoke-test.sh` 28 / 28 |

---

## 10. 故障排除

**怎麼用這張表**：先看症狀對應的「看哪裡」——Redis 出問題時答案幾乎都在 `journalctl -u redis`（systemd 層：起不來）或 `/var/log/redis/redis.log`（Redis 層：起來了但不對）。
下面每一列都是本章某個設定沒做到位的直接後果，對照回去就知道該補哪一步。

| 症狀 | 看哪裡 | 常見原因（對應章節） |
|---|---|---|
| `systemctl start redis` 卡住後 timeout | `journalctl -u redis` | `Type=notify` 但 redis.conf 沒有 `supervised systemd`，或 binary 不是用 `USE_SYSTEMD=yes` 編的——systemd 在等一個永遠不會來的通知 → 改 `Type=simple`（第 6 節） |
| `Permission denied` 開 log / 資料目錄 | `ls -l /var/lib/redis /var/log/redis` | 目錄 owner 不是 redis；或 `ProtectSystem` 擋住不在 `ReadWritePaths` 的路徑（第 4 節步驟 2、第 6 節） |
| `Can't save in background: fork: Cannot allocate memory` | `sysctl vm.overcommit_memory` | 要設 1（第 7 節） |
| 啟動就退出 `Bind: Address already in use` | `ss -tlnp \| grep 6379` | port 被占（第 4 節步驟 1） |
| `NOAUTH Authentication required` | | 要 `-a` 密碼；或 ACL 使用者（第 5 節） |
| `DENIED Redis is running in protected mode` | | 沒設密碼卻從非 loopback 連——protected-mode 在保護你（第 5 節） |
| `MISCONF ... RDB snapshots ... unable to persist` | `df -h`、日誌 | 磁碟滿或目錄不可寫，Redis 拒寫以免你以為資料有存；緊急時 `CONFIG SET stop-writes-on-bgsave-error no` 先恢復服務，再修磁碟 |
| 延遲突然變高 | `redis-cli --latency`、`latency doctor`、`INFO stats` 的 `latest_fork_usec` | fork、THP、swap、慢指令（第 7 節 → [04](04-performance-tuning.md)） |
