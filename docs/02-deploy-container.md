# 02 · 容器佈署：Docker / Docker Compose

> **這一章要解決的問題**：你想在自己的電腦上，用最少的步驟拿到一個「和正式環境行為一致」的 Redis 來練習——
> 包括單機、有自動故障轉移的 Sentinel、有分片的 Cluster——而且希望它們是**真的能用**的，不是貼上去會壞的範例。
>
> 本專案附三套 Compose 環境，全部在 Docker 29 / Compose v5 / Redis 8.10.1 實測過（見 [驗證](#8-驗證)）。
> 這一章的每一段都先講「為什麼要這樣設」，再講「怎麼做」；跳過 Why 直接抄設定，遇到容器特有的坑時你會不知道從哪裡查起。

## 目錄

1. [三套環境一覽](#1-三套環境一覽)
2. [單機環境：docker-compose.yml](#2-單機環境docker-composeyml)
3. [Sentinel 環境：docker-compose-sentinel.yml](#3-sentinel-環境docker-compose-sentinelyml)
4. [Cluster 環境：docker-compose-cluster.yml](#4-cluster-環境docker-compose-clusteryml)
5. [容器環境的六個重點](#5-容器環境的六個重點)
6. [監控：redis_exporter → Prometheus → Grafana](#6-監控redis_exporter--prometheus--grafana)
7. [常見錯誤](#7-常見錯誤)
8. [驗證](#8-驗證)
9. [容器還是 VM？](#9-容器還是-vm)

---

## 1. 三套環境一覽

**為什麼要三套，而不是一套？**
Redis 的三種拓撲（單機、Sentinel、Cluster）解決的是不同問題：單機解決「我要一個 Redis」，Sentinel 解決「Master 掛了要有人自動接手」，
Cluster 解決「資料量或寫入量超過一台機器」（拓撲選型的推理見 [01 §6](01-architecture.md#6-四種拓撲單機主從sentinelcluster)）。
它們的設定檔、埠號、故障行為完全不同，硬塞進一個 Compose 檔只會讓初學者搞不清楚「現在在練哪一個」。
所以本專案一種拓撲一個檔案，各自能獨立啟動、獨立驗證、獨立關閉。

**怎麼分**：三套都宣告了 Compose 的 `name:`，因此是三個獨立專案，容器名稱與網路互不干擾，可以同時存在。
唯一的例外是宿主機埠號——**單機與 Sentinel 都把 Master 映射到宿主機 6379**，兩者不要同時啟動（否則第二個會因 port 被占用而起不來）。

| 環境 | 檔案 | 節點 | 埠號 | 用途 | 驗證腳本 |
|---|---|---|---|---|---|
| 單機 | `docker-compose.yml` | Redis + RedisInsight + exporter + Prometheus + Grafana | 6379 / 5540 / 9121 / 9090 / 3000 | Module 01–08、11–14 的手動練習 | `scripts/verify-single.sh` |
| Sentinel | `docker-compose-sentinel.yml` | 1 Master + 2 Replica + 3 Sentinel | 6379–6381 / 26379–26381 | Module 09 高可用 | `scripts/verify-sentinel.sh` |
| Cluster | `docker-compose-cluster.yml` | 3 Master + 3 Replica | 7001–7006（bus 17001–17006） | Module 10 分片 | `scripts/verify-cluster.sh` |

**如果你只是想先看到 PONG**：還不需要 Compose。官方映像 `redis:8` 已內建 JSON / Search / Bloom / TimeSeries 模組，一行就能起來：

```bash
docker run -d --name redis -p 6379:6379 redis:8          # 含模組；redis:8-alpine 只有核心、體積小
docker exec -it redis redis-cli ping                       # PONG
docker rm -f redis
```

但這個容器沒有設定檔、沒有 volume、沒有監控——資料隨容器消失。下面三節就是把這些補齊。

---

## 2. 單機環境：docker-compose.yml

**這一節要解決的問題**：一個能練所有基本指令、關掉再開資料還在、而且看得到內部指標的 Redis。
`docker run` 做不到的三件事——掛設定檔、掛資料 volume、把監控工具接上——Compose 一次做完。

### 2.1 怎麼啟動

**為什麼有「全部」和「只要 Redis」兩種啟動法**：Prometheus 與 Grafana 各吃幾百 MB 記憶體，也要占 9090 / 3000 埠；
練 Module 01–08 只需要 Redis 和 GUI，把監控一起拉起來只是浪費資源、增加埠號衝突的機會。等到讀 [04 效能最佳化](04-performance-tuning.md) 要看指標時再全開。

```bash
docker compose up -d                       # 全部：Redis + RedisInsight + exporter + Prometheus + Grafana
docker compose up -d redis redis-insight   # 只要 Redis 和 GUI（平常練習用這個）
docker compose ps                          # 看狀態；redis 應顯示 (healthy)
docker compose logs -f redis               # Redis 的啟動日誌（WARNING 會出現在這裡）
docker compose exec redis redis-cli        # 進入 CLI
docker compose down                        # 停止；資料留在 volume
docker compose down -v                     # 停止並刪除 volume（資料一起消失，確定不要了再用）
```

**為什麼 9090 / 3000 可以用環境變數改**：這兩個埠號是 Prometheus / Grafana 的慣例，同一台機器上很常已經有別的專案在用（本專案實測的機器就是這樣）。
與其改檔案，不如啟動時覆寫：

```bash
PROMETHEUS_PORT=19090 GRAFANA_PORT=13000 docker compose up -d
```

### 2.2 設定檔為什麼長這樣

設定檔在 `docker/redis/redis.conf`。它只有幾行，但每一行都對應一個「不設會怎樣」：

| 設定 | 為什麼要這樣設 | 不設會怎樣 |
|---|---|---|
| `bind 0.0.0.0` | 容器有自己的網路命名空間，Redis 若只綁 127.0.0.1，宿主機透過 port 映射連進來會被拒絕。對外的安全邊界由 Docker 的 `ports:` 決定，不是這一行 | 宿主機 `redis-cli` 連不上 |
| `protected-mode no` | 練習環境沒設密碼；`protected-mode yes` 會拒絕所有非 loopback 的連線，配上 `bind 0.0.0.0` 等於誰都連不上 | 同上；**正式環境要開密碼而不是關 protected-mode**，見 [06](06-operations.md) |
| `maxmemory 256mb` | 練習資料不需要更多；更重要的是養成「一定要設上限」的習慣，不設的 Redis 會吃到被 OOM killer 殺掉 | 記憶體無上限 |
| `maxmemory-policy allkeys-lru` | 練習用途把它當快取：滿了淘汰最久沒用的 key，而不是拒絕寫入 | 預設 `noeviction`，滿了寫入報 OOM |
| `appendonly yes` + `aof-use-rdb-preamble yes` | 希望 `docker compose restart` 之後資料還在。AOF 最多丟 1 秒；混合模式讓重啟載入快 | 只剩 RDB 快照，重啟可能掉最近幾分鐘的資料 |

改了設定檔要 `docker compose restart redis` 才生效（Redis 只在啟動時讀檔）。
臨時試某個值可以線上 `CONFIG SET`，但它不會寫回檔案，重啟後消失——這是「試」與「定案」的分工。

### 2.3 RedisInsight

**為什麼要用 GUI**：初學階段最大的障礙是「看不到資料長什麼樣」。RedisInsight 把 key 用冒號展開成樹狀、顯示每個 key 的型別與 TTL，
還內建 CLI 與慢查詢分析。它是 Redis 官方工具，本專案直接放進 Compose。

開 <http://localhost:5540>，第一次要手動新增連線——**host 填 `redis`（容器名稱），不是 localhost**：
RedisInsight 自己也在容器裡，它的 localhost 是它自己，不是 Redis；同一個 Compose 網路內，服務名稱就是 DNS 名稱（[§5.5](#55-網路名稱解析固定-ip埠映射)）。

---

## 3. Sentinel 環境：docker-compose-sentinel.yml

**這一節要解決的問題**：Master 掛掉時，不用人介入就有 Replica 自動升為 Master。
這需要 1 個 Master、至少 2 個 Replica（一個接手、一個留著當備援）、3 個 Sentinel（投票要過半數，2 個會平手、1 個沒有容錯）。
本環境就是這個最小組合，重點不在「起得來」，而在**真的把 Master 停掉看它切換**。

```bash
docker compose -f docker-compose-sentinel.yml up -d
./scripts/verify-sentinel.sh                       # 含自動故障轉移測試（實測 7 秒完成切換）

# 手動觀察
docker exec sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster   # 誰是 Master
docker exec sentinel-1 redis-cli -p 26379 SENTINEL replicas mymaster
docker compose -f docker-compose-sentinel.yml stop redis-master                       # 模擬 Master 掛掉
docker logs -f sentinel-1                          # 看 +sdown → +odown → +switch-master
docker compose -f docker-compose-sentinel.yml start redis-master                      # 回來會變 Replica
docker compose -f docker-compose-sentinel.yml down -v
```

### 3.1 為什麼要固定 IP：實測踩到的坑 1

這是本專案**修正過**的 bug，值得從頭理解，因為它是容器環境獨有的：

**問題**：原本 `sentinel.conf` 寫 `sentinel monitor mymaster redis-master 6379 2`，用容器名稱指 Master。
第一個症狀是三個 Sentinel 啟動即崩潰（`Can't resolve instance hostname`）——Sentinel 預設不做主機名稱解析。
加上 `sentinel resolve-hostnames yes` 之後能啟動了，但**Master 一停，Sentinel 就進入 TILT 模式、永遠不切換**。

**原因**：Master 容器停掉後，Docker 的內建 DNS 立刻把 `redis-master` 這個名稱撤掉。Sentinel 每秒都要解析一次這個名稱，
解析失敗的等待時間超過 2 秒，Sentinel 偵測到自己的事件迴圈被卡住，判斷「我的時間感不可信」而進入 TILT 保護模式——在這個模式下它拒絕做任何故障判斷。
換句話說，**Master 掛掉本身就摧毀了 Sentinel 用來偵測 Master 掛掉的機制**。

**做法**：不用名稱，用 IP。Compose 建一個固定子網 `172.28.0.0/24`，每個節點指定 `ipv4_address`，Sentinel 監控 `172.28.0.10`。
IP 不依賴 DNS，Master 停掉只是連不上，不會卡住 Sentinel。

```yaml
sentinel-1:
  networks:
    sentinel-net:
      ipv4_address: 172.28.0.21
networks:
  sentinel-net:
    ipam:
      config:
        - subnet: 172.28.0.0/24
```

### 3.2 為什麼設定檔要先複製再啟動：實測踩到的坑 2

**問題**：Sentinel 與一般 Redis 不同，它會**改寫自己的設定檔**——把自己的 `myid`、發現的 Replica 與其他 Sentinel 都寫回去，這是它記住拓撲的方式。
原本三個 Sentinel 用 bind mount 掛同一個 `docker/sentinel/sentinel.conf`，結果三個行程輪流覆寫同一個宿主機檔案：
`myid` 互相覆蓋導致它們以為彼此是同一個 Sentinel，而且檔案 owner 被改成 root，之後你在宿主機連編輯都不行（`permission denied`）。

**做法**：設定檔以唯讀掛到 `/tmp`，`entrypoint` 先複製到容器內可寫的位置再啟動，每個 Sentinel 改寫的是自己的副本：

```yaml
sentinel-1:
  volumes:
    - ./docker/sentinel/sentinel.conf:/tmp/sentinel.conf:ro
  entrypoint: ["sh", "-c", "cp /tmp/sentinel.conf /data/sentinel.conf && exec redis-sentinel /data/sentinel.conf"]
```

### 3.3 三個關鍵參數的取捨

Sentinel 的設定基本上就是在「誤判」與「切換慢」之間選一個點。先理解每個參數在調什麼，再看本環境的值：

| 參數 | 它在調什麼 | 調大會怎樣 | 調小會怎樣 | 本環境 |
|---|---|---|---|---|
| `quorum`（`monitor` 最後一個數字） | 幾個 Sentinel 同意「Master 掛了」才算數（客觀下線） | 更難誤判，但少數 Sentinel 掛掉就無法切換 | 一個 Sentinel 網路抖動就可能誤切 | 2（3 個裡的過半） |
| `down-after-milliseconds` | 多久沒回應算「我認為它掛了」（主觀下線） | 切換慢 | 短暫 GC / 網路抖動就誤判 | 5000 |
| `failover-timeout` | 一次切換的逾時；也決定同一個 Master 兩次 failover 的最小間隔 | 失敗的切換要等更久才重試 | 太短會在切換尚未完成時重來 | 10000 |

實測從 `stop redis-master` 到 `+switch-master` 約 **7 秒** = 5 秒 `down-after` + 投票與升級約 2 秒。這就是這組參數下的 RTO。

---

## 4. Cluster 環境：docker-compose-cluster.yml

**這一節要解決的問題**：資料太多、寫入太多，一台 Redis 放不下。Cluster 把 16384 個 slot 分給多個 Master，每個 Master 再配一個 Replica 做容錯。
最小的正式配置是 3 主 3 從，本環境就是這個大小——**6 個節點是下限**，少於 3 個 Master 投票無法過半，少了 Replica 一個 Master 掛掉就掉一段 slot。

```bash
docker compose -f docker-compose-cluster.yml up -d      # redis-cluster-init 容器會自動 --cluster create
docker logs redis-cluster-init                          # [OK] All 16384 slots covered.
./scripts/verify-cluster.sh                             # 含停掉一個 Master → Replica 接手（實測 9 秒）

docker exec -it redis-node-1 redis-cli -c -p 7001       # -c：自動跟隨 MOVED
> CLUSTER INFO
> CLUSTER NODES
> SET user:1 a          # -> Redirected to slot [...] located at 172.29.0.12:7002
> CLUSTER KEYSLOT user:1
docker compose -f docker-compose-cluster.yml down -v
```

### 4.1 為什麼初始化要另一個容器

**問題**：6 個 Redis 起來之後只是 6 台互不認識的機器，要有人執行 `redis-cli --cluster create` 把它們組成叢集、分配 slot、指定誰是誰的 Replica。
這件事只做一次、而且要等 6 個節點都能連才行。

**做法**：一個一次性容器 `redis-cluster-init`，`depends_on` 六個節點、`sleep 5` 等它們就緒、執行 `--cluster create ... --cluster-replicas 1 --cluster-yes`，做完自動退出。
`docker logs redis-cluster-init` 看到 `[OK] All 16384 slots covered.` 就是成功。

### 4.2 為什麼也要固定 IP：實測踩到的坑 3

**問題**：Cluster 節點靠 `cluster-announce-ip` 告訴其他節點「我在哪」。原本這裡填容器名稱 `redis-node-N`。
建立叢集沒問題，但 `verify-cluster.sh` 停掉一個 Master 之後，**整個叢集變成 `cluster_state:fail`，Replica 三分鐘都沒升級**，連沒被停掉的節點回應都變慢。

**原因**：和 Sentinel 的坑同源。節點停掉、名稱從 DNS 消失，其他節點在主執行緒裡嘗試解析這個名稱重連，每次都要等 DNS 逾時——
Redis 是單執行緒，這段等待讓整個節點停擺，選舉需要的心跳也送不出去。

**做法**：`172.29.0.0/24` 固定 IP，`--cluster-announce-ip 172.29.0.1N`。改完後同一個測試 9 秒內 Replica 升級、`cluster_state:ok`。

### 4.3 為什麼要在容器內操作

Cluster 的 client 會收到 `MOVED 5712 172.29.0.12:7002` 這種重新導向，然後**自己連到那個位址**。
從宿主機用 `redis-cli -c -p 7001`，第一步能連（有 port 映射），被導向到 `172.29.0.12` 時就要看宿主機能不能路由到 Docker 子網——
Linux 原生 Docker 可以，Docker Desktop（Mac / Windows）不行。所以練習時一律 `docker exec -it redis-node-1 redis-cli -c -p 7001`，行為在所有平台一致。

---

## 5. 容器環境的六個重點

**這一節要解決的問題**：Redis 本身的設定容器與 VM 幾乎一樣，差別全在「容器這一層」——資料放哪、記憶體邊界在哪、核心參數誰管、名稱怎麼解析。
這六件事在 VM 上不用想，在容器裡不想就會出事。

### 5.1 資料放哪裡：一定要 volume

**為什麼**：容器的檔案系統是暫時的，`docker rm` 之後 `/data` 裡的 RDB / AOF 一起消失——你以為開了 AOF 就安全，其實檔案根本沒留下來。

**怎麼做**：把 `/data` 掛到一個 named volume。named volume 由 Docker 管理，`docker compose down` 不會刪，只有 `down -v` 才會：

```yaml
volumes:
  - redis-data:/data        # named volume：down 不刪，down -v 才刪
```

`docker volume ls`、`docker volume inspect redis-tutorial_redis-data` 可以看到實際存放路徑；備份時就是複製那裡的檔案（[06 §5](06-operations.md#5-備份與還原)）。

### 5.2 設定檔：bind mount + 唯讀

**為什麼要用檔案而不是命令列參數**：`redis-server --maxmemory 256mb --appendonly yes` 也行，但設定一多就不可讀、也無法加註解。
把 `redis.conf` 放在 repo 裡用 bind mount 掛進去，設定就跟程式碼一起進版控、有 diff、有歷史。

**為什麼要 `:ro`**：容器內的行程若能寫這個檔案，就可能改掉宿主機的版本——Sentinel 那個坑就是這樣來的。一般 Redis 不會改寫設定檔，但加 `:ro` 是零成本的保險。

```yaml
volumes:
  - ./docker/redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
```

### 5.3 記憶體：容器限制要大於 maxmemory

**為什麼兩個上限不能一樣**：`maxmemory` 到了，Redis 會依策略淘汰 key，服務照常。
但容器的 memory limit 是 cgroup 的硬牆——到了就是 OOM kill，**Redis 直接消失、沒有任何日誌**，你只會看到容器不斷重啟。
而 Redis 做 RDB / AOF rewrite 時要 fork，fork 期間寫入越多、copy-on-write 複製的頁越多，實際用量會**超過** `maxmemory`。

**怎麼做**：容器限制至少是 `maxmemory` 的 1.5–2 倍：

```yaml
deploy:
  resources:
    limits:
      memory: 512M     # maxmemory 256mb 的 2 倍，留給 fork 的 copy-on-write
```

### 5.4 核心參數是宿主機的事

**為什麼容器裡改不了**：Redis 啟動時會檢查三個核心設定並發 WARNING——`vm.overcommit_memory`、Transparent Huge Pages、`net.core.somaxconn`。
前兩個是全機共用的核心參數，屬於宿主機的核心，容器沒有權限改；它們影響的是 fork 能不能成功、fork 後延遲會不會飆（理由見 [03 §7](03-deploy-vm.md#7-os-調校)）。

**怎麼做**：在宿主機改（Docker Desktop / WSL2 的話是那台 Linux VM）：

```bash
sudo sysctl -w vm.overcommit_memory=1
sudo sysctl -w net.core.somaxconn=1024
echo never | sudo tee /sys/kernel/mm/transparent_hugepage/enabled
```

`net.*` 命名空間的參數是例外——它是每個網路命名空間各有一份，所以 Compose 可以替容器單獨設：

```yaml
sysctls:
  net.core.somaxconn: 1024
```

沒改的話 `docker logs redis` 會看到 `WARNING Memory overcommit must be enabled!`。練習環境可以忽略；正式環境跑在容器裡（例如 Kubernetes）就要在節點層處理。

### 5.5 網路：名稱解析、固定 IP、埠映射

**三個規則，對應三個常見困惑**：

1. **同一個 Compose 網路內，服務名稱就是 DNS 名稱**——所以 RedisInsight 連 `redis`、Replica 的 `replicaof` 可以寫 `redis-master`。這是最方便的方式。
2. **容器停掉，名稱就從 DNS 消失**——所以任何「節點消失後仍要能辨識它」的元件（Sentinel、Cluster 節點）必須用固定 IP，不能用名稱。§3.1 與 §4.2 就是這個規則的代價。
3. **`ports: "6379:6379"` 是給宿主機用的**——容器之間互連走內部網路，不需要映射。正式環境若只有同網路的應用要連 Redis，不映射反而更安全（宿主機以外根本連不到）。

### 5.6 日誌與健康檢查

**為什麼日誌要走 stdout**：容器慣例是日誌交給 runtime 收集（`docker logs`、K8s 的 log driver）。Redis 若寫到容器內的檔案，容器一刪日誌就沒了，也沒人會去看。

```conf
logfile ""              # 空字串 = stdout → docker logs 看得到
```

**為什麼要 healthcheck**：容器「在跑」不等於 Redis「能服務」——載入大 AOF 的那幾十秒行程存在但回 `LOADING`。
healthcheck 用 `redis-cli ping` 定義「能服務」，其他服務用 `depends_on: condition: service_healthy` 等它真的能回 PONG 再啟動，
否則 RedisInsight / exporter 會在 Redis 還沒好時連線失敗。

```yaml
healthcheck:
  test: ["CMD", "redis-cli", "ping"]      # 有密碼：["CMD", "redis-cli", "-a", "密碼", "--no-auth-warning", "ping"]
  interval: 10s
  timeout: 5s
  retries: 5
restart: unless-stopped                   # 掛了自動拉起；手動 stop 的不拉
```

---

## 6. 監控：redis_exporter → Prometheus → Grafana

**這一節要解決的問題**：`INFO` 指令只能看「現在」，看不到趨勢——記憶體是慢慢漲還是突然爆、命中率什麼時候掉的、故障轉移前有沒有徵兆。
要回答這些問題得把指標定期抓下來畫成圖。Redis 沒有內建這個能力，所以要三個元件接力：

```
Redis :6379 ──INFO──▶ redis_exporter :9121 ──scrape──▶ Prometheus :9090 ──query──▶ Grafana :3000
```

- **redis_exporter**：每次被抓時對 Redis 執行 `INFO`，翻譯成 Prometheus 格式。它是 Redis 與 Prometheus 之間的翻譯官，沒有它 Prometheus 看不懂 Redis。
- **Prometheus**：每 15 秒抓一次並存成時間序列（`docker/prometheus/prometheus.yml`）。另有一個 `spring-boot` job 抓 `host.docker.internal:8080/actuator/prometheus`——
  這個名稱在 Mac / Windows 的 Docker Desktop 內建，Linux 沒有，所以 Compose 加了 `extra_hosts: host-gateway` 讓 Linux 也能解析。
- **Grafana**：畫圖。`docker/grafana/provisioning/` 讓它啟動時自動建立 Prometheus 資料來源與 **Redis Tutorial** 儀表板（`docker/grafana/dashboards/redis-overview.json`），
  不用手動點；帳密 `admin / admin` → Dashboards → Redis → Redis Tutorial。

儀表板上的 14 個面板不是隨便挑的——它們一一對應 [04 §8](04-performance-tuning.md#8-監控指標與告警門檻) 的告警門檻：ops/s、記憶體 vs maxmemory、命中率、淘汰數、各指令平均延遲、碎片率、複寫延遲、距上次 RDB 存檔秒數。

沒有 Grafana 也能直接看 exporter 吐出來的原始指標，確認鏈路的第一環是通的：

```bash
curl -s localhost:9121/metrics | grep -E "^redis_(up|connected_clients|memory_used_bytes|commands_processed_total)"
```

---

## 7. 常見錯誤

**怎麼用這張表**：先看「原因」欄理解為什麼會發生，再做處理；同一個症狀常有不同原因（例如 Sentinel 不切換有 DNS 與 quorum 兩種可能）。

| 症狀 | 原因 | 處理 |
|---|---|---|
| `Bind for 0.0.0.0:6379 failed: port is already allocated` | 宿主機已有 Redis 占用 6379（另一套 Compose、VM 安裝、brew）；Docker 不會幫你搶 | `ss -tlnp \| grep 6379` 找出來停掉，或改映射 `"16379:6379"` |
| `Can't resolve instance hostname`（Sentinel 起不來） | `sentinel.conf` 用主機名稱但沒開 `resolve-hostnames`；即使開了也會遇到下一列 | 本專案已改為固定 IP（§3.1） |
| Sentinel 一直 `+tilt`、不 failover | Master 名稱解析阻塞讓 Sentinel 事件迴圈被卡 > 2 秒 | 用 IP（§3.1） |
| `permission denied` 編輯 `docker/sentinel/sentinel.conf` | 舊版 Compose 讓 Sentinel 以 root 改寫了它 | `sudo chown $USER docker/sentinel/sentinel.conf`；新版已先複製再啟動（§3.2） |
| `MISCONF Redis is configured to save RDB snapshots...` | `/data` 不可寫或磁碟滿，Redis 為了保護資料拒絕寫入 | 檢查 volume 權限、磁碟空間 |
| `WARNING Memory overcommit must be enabled` | 宿主機核心參數，容器內改不了 | §5.4；練習可忽略 |
| Cluster：宿主機 `redis-cli -c` 被導到 `172.29.0.12:7002` 連不上 | 重新導向的目標是 Docker 子網 IP，Docker Desktop 的宿主機路由不到 | 在容器內操作（§4.3） |
| Cluster 停掉一個節點後 `cluster_state:fail` 且不恢復 | `cluster-announce-ip` 用了容器名稱，DNS 阻塞主執行緒 | 本專案已改固定 IP（§4.2） |
| `(error) CROSSSLOT Keys in request don't hash to the same slot` | Cluster 的多 key 指令必須落在同一節點，否則無法原子執行 | 用 hash tag `{...}` 讓相關 key 同 slot |
| Testcontainers 起不來 | Docker socket 權限 / Docker 沒跑；Java 測試會自己起容器 | `docker ps` 先確認；README 常見問題 |

---

## 8. 驗證

**為什麼要有驗證腳本，而不是「起得來就好」**：本專案原本的 Sentinel 與 Cluster 環境都「起得來」——容器全綠、`ps` 正常——
但 Master 一停就不會切換。這種 bug 只有**真的把節點停掉**才會現形。所以每套環境的驗證腳本都包含故障注入，不只檢查靜態狀態。

三套環境各有一支驗證腳本，`scripts/verify-all.sh` 會依序啟動、驗證、關閉：

```bash
./scripts/verify-all.sh          # 約 3–4 分鐘；需要 6379、6380–6381、26379–26381、7001–7006 空著
```

| 腳本 | 驗證項目 | 為什麼驗這些 |
|---|---|---|
| `verify-single.sh`（11 項） | 容器 healthy、`redis.conf` 每個關鍵設定生效、模組已載入（JSON.SET）、**重啟容器後資料仍在**、RedisInsight / exporter / Prometheus / Grafana 端點 | 設定檔掛錯路徑時 Redis 會用預設值靜靜啟動，只有讀回設定值才知道有沒有生效；重啟測試證明 volume 與 AOF 真的在工作 |
| `verify-sentinel.sh`（18 項） | 6 個容器都在、Sentinel 互相發現、Replica 同步且唯讀、**停掉 Master → 幾秒內 `+switch-master`**、新 Master 有資料且可寫、**舊 Master 回來被降為 Replica 並完成同步** | 高可用的唯一證明是故障時真的切換；舊 Master 回歸若沒被降級會造成雙 Master（腦裂） |
| `verify-cluster.sh`（14 項） | `cluster_state:ok`、16384 slot 全指派、3 主 3 從、hash tag 同 slot、不帶 `-c` 收到 MOVED、`CROSSSLOT` 行為、**停掉一個 Master → Replica 接手、資料仍在、回歸後變 Replica** | slot 沒全覆蓋的叢集會對部分 key 回 CLUSTERDOWN；MOVED / CROSSSLOT 是應用程式一定會遇到的行為，先在這裡看過 |

實測結果（單機 11/11、Sentinel 18/18 且 7 秒切換、Cluster 14/14 且 9 秒接手）記錄在 [README 的驗證章節](../README.md#驗證方式與實測紀錄)。

---

## 9. 容器還是 VM？

**這一節要解決的問題**：學完容器環境之後，正式環境該選哪一種？答案取決於「誰負責讓行程活著」和「誰能調 OS 參數」，而不是哪個比較潮。

| 面向 | 容器 | VM（[03](03-deploy-vm.md)） | 為什麼有差 |
|---|---|---|---|
| 適合 | 學習、開發、CI、Kubernetes 上的正式環境 | 傳統正式環境、效能基準、需要完整 OS 調校 | 容器省事，VM 可控 |
| 行程管理 | Docker / K8s 的 restart policy、探針 | systemd（graceful shutdown、資源限制、日誌） | systemd 的 `TimeoutStopSec` 能保證 Redis 有時間把資料寫完；容器的 stop 預設只等 10 秒 |
| OS 調校 | 受宿主機限制（overcommit、THP 改不了） | sysctl / THP / ulimit 全部可控 | §5.4：fork 相關的參數在容器裡碰不到 |
| 資料 | volume（掛錯就丟） | 直接在 VM 磁碟 | volume 是額外的一層抽象，多一個出錯點 |
| 記憶體 | cgroup limit 是硬牆，OOM 沒有日誌 | 整台機器的記憶體，OOM killer 有日誌 | §5.3：容器被 OOM kill 時你只看到重啟 |
| 升級 | 換 image tag 重啟 | 換 binary / 套件重啟 | 容器的回滾就是改回 tag |

兩者的 `redis.conf` 幾乎一樣——你在這一章學到的設定原則到 VM 全部適用，差別只在外面那一層。
