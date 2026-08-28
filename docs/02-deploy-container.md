# 02 · 容器佈署：Docker / Docker Compose

> 本專案附三套 Compose 環境，全部在 Docker 29 / Compose v5 / Redis 8.10.1 實測過（見 [驗證](#8-驗證)）。
> 這一章教你怎麼用、每個設定為什麼這樣寫、容器環境特有的坑在哪裡。

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

| 環境 | 檔案 | 節點 | 埠號 | 用途 | 驗證腳本 |
|---|---|---|---|---|---|
| 單機 | `docker-compose.yml` | Redis + RedisInsight + exporter + Prometheus + Grafana | 6379 / 5540 / 9121 / 9090 / 3000 | Module 01–08、11–14 的手動練習 | `scripts/verify-single.sh` |
| Sentinel | `docker-compose-sentinel.yml` | 1 Master + 2 Replica + 3 Sentinel | 6379–6381 / 26379–26381 | Module 09 高可用 | `scripts/verify-sentinel.sh` |
| Cluster | `docker-compose-cluster.yml` | 3 Master + 3 Replica | 7001–7006（bus 17001–17006） | Module 10 分片 | `scripts/verify-cluster.sh` |

三套都用了 Compose 的 `name:`，所以是獨立專案，可以同時存在；
但**單機與 Sentinel 都用宿主機 6379**，兩者不要同時啟動。

最快的起手式（不需要 Compose）：

```bash
docker run -d --name redis -p 6379:6379 redis:8          # 含 JSON/Search/Bloom/TimeSeries 模組
docker exec -it redis redis-cli ping                       # PONG
docker rm -f redis
```

---

## 2. 單機環境：docker-compose.yml

```bash
docker compose up -d                       # 全部：Redis + RedisInsight + exporter + Prometheus + Grafana
docker compose up -d redis redis-insight   # 只要 Redis 和 GUI
docker compose ps
docker compose logs -f redis
docker compose exec redis redis-cli        # 進入 CLI
docker compose down                        # 停止（資料保留在 volume）
docker compose down -v                     # 停止並刪除資料
```

> 9090（Prometheus）或 3000（Grafana）被占用時：
> `PROMETHEUS_PORT=19090 GRAFANA_PORT=13000 docker compose up -d`

設定檔在 `docker/redis/redis.conf`，重點：

```conf
bind 0.0.0.0            # 容器內要綁所有介面，對外由 docker 的 port 映射決定
protected-mode no       # 練習環境沒設密碼；正式環境請看 docs/06-operations.md
maxmemory 256mb
maxmemory-policy allkeys-lru
appendonly yes          # + aof-use-rdb-preamble yes：混合持久化，重啟不丟資料
```

改了設定檔要 `docker compose restart redis` 才生效；或線上 `CONFIG SET`（重啟後消失）。

RedisInsight：<http://localhost:5540>，第一次要手動新增連線：host 填 `redis`（容器名稱）、port 6379。

---

## 3. Sentinel 環境：docker-compose-sentinel.yml

```bash
docker compose -f docker-compose-sentinel.yml up -d
./scripts/verify-sentinel.sh                       # 含自動故障轉移測試

# 手動觀察
docker exec sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster   # 誰是 Master
docker exec sentinel-1 redis-cli -p 26379 SENTINEL replicas mymaster
docker compose -f docker-compose-sentinel.yml stop redis-master                       # 模擬 Master 掛掉
docker logs -f sentinel-1                           # 看 +sdown → +odown → +switch-master
docker compose -f docker-compose-sentinel.yml start redis-master                      # 回來會變 Replica
docker compose -f docker-compose-sentinel.yml down -v
```

這個環境有兩個**實測踩到、已經修好**的坑，值得知道：

**坑 1：Sentinel 用容器名稱監控 Master，Master 一停就進 TILT 模式、不切換。**
Master 容器停掉後 Docker DNS 解析不到 `redis-master`，Sentinel 的 DNS 查詢阻塞超過 2 秒，
觸發 TILT 保護模式（暫停所有故障判斷）。解法：Compose 給每個節點**固定 IP**（`172.28.0.0/24`），
`sentinel monitor mymaster 172.28.0.10 6379 2` 用 IP。

**坑 2：三個 Sentinel 共用同一個 bind-mount 的 sentinel.conf。**
Sentinel 會改寫自己的設定檔（寫入 `myid`、發現的節點）。三個容器寫同一個宿主機檔案會互相覆蓋、
還會把檔案 owner 改成 root。解法：`entrypoint` 先 `cp` 到容器內可寫位置再啟動。

```yaml
sentinel-1:
  volumes:
    - ./docker/sentinel/sentinel.conf:/tmp/sentinel.conf:ro
  entrypoint: ["sh", "-c", "cp /tmp/sentinel.conf /data/sentinel.conf && exec redis-sentinel /data/sentinel.conf"]
  networks:
    sentinel-net:
      ipv4_address: 172.28.0.21
```

Sentinel 三個關鍵參數：

| 參數 | 本環境 | 意義 |
|---|---|---|
| `quorum`（monitor 最後一個數字） | 2 | 幾個 Sentinel 同意 Master 下線才算客觀下線 |
| `down-after-milliseconds` | 5000 | 多久沒回應算主觀下線；太小會誤判、太大切換慢 |
| `failover-timeout` | 10000 | 一次故障轉移的逾時；也是同一 Master 兩次 failover 的最小間隔 ×2 |

---

## 4. Cluster 環境：docker-compose-cluster.yml

```bash
docker compose -f docker-compose-cluster.yml up -d      # redis-cluster-init 容器會自動 --cluster create
docker logs redis-cluster-init                          # [OK] All 16384 slots covered.
./scripts/verify-cluster.sh

docker exec -it redis-node-1 redis-cli -c -p 7001       # -c：自動跟隨 MOVED
> CLUSTER INFO
> CLUSTER NODES
> SET user:1 a          # -> Redirected to slot [...] located at redis-node-2:7002
> CLUSTER KEYSLOT user:1
docker compose -f docker-compose-cluster.yml down -v
```

每個節點用 `--cluster-announce-ip redis-node-N` 宣告自己的位址（容器名稱，Redis 7+ 可解析），
其他節點與 client 就能用這個名稱連過來。從**宿主機**用 `redis-cli -c -p 7001` 會被導向到 `redis-node-2:7002`，
宿主機解析不到這個名稱——所以練習時請在容器內操作，或在 `/etc/hosts` 加上 `127.0.0.1 redis-node-1 ... redis-node-6`。

---

## 5. 容器環境的六個重點

### 5.1 資料放哪裡：一定要 volume

```yaml
volumes:
  - redis-data:/data        # named volume：docker compose down 不會刪，down -v 才會
```

沒掛 volume 的 Redis 容器一刪，RDB / AOF 就跟著消失。`docker volume ls`、`docker volume inspect redis-tutorial_redis-data` 可以看實際路徑。

### 5.2 設定檔：bind mount + 唯讀

```yaml
volumes:
  - ./docker/redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
```

`:ro` 防止容器改寫宿主機檔案（Sentinel 就是反例）。
不想維護設定檔時，也可以全部用命令列：`command: redis-server --maxmemory 256mb --appendonly yes`。

### 5.3 記憶體：容器限制要大於 maxmemory

```yaml
deploy:
  resources:
    limits:
      memory: 512M     # 要 > maxmemory（256mb）+ fork 的 copy-on-write 空間
```

`maxmemory` 到了 Redis 會淘汰；**容器 memory limit** 到了是 cgroup OOM kill，Redis 直接消失、沒有日誌。
兩者至少差 1.5–2 倍。

### 5.4 核心參數是宿主機的事

Redis 啟動時會檢查三個核心設定，容器內**改不了**，要在宿主機改：

```bash
# 宿主機（Docker Desktop / WSL2 的話是那台 Linux VM）
sudo sysctl -w vm.overcommit_memory=1
sudo sysctl -w net.core.somaxconn=1024
echo never | sudo tee /sys/kernel/mm/transparent_hugepage/enabled
```

Compose 的 `sysctls:` 只能改 `net.*` 命名空間內的值：

```yaml
sysctls:
  net.core.somaxconn: 1024
```

沒改的話 `docker logs redis` 會看到 `WARNING Memory overcommit must be enabled!`，練習環境可以忽略。

### 5.5 網路：名稱解析、固定 IP、埠映射

- 同一個 Compose 網路內，服務名稱就是 DNS 名稱（`redis`、`redis-master`）
- 容器停掉，名稱就解析不到——需要「節點消失後仍可辨識」的元件（Sentinel）要用固定 IP
- `ports: "6379:6379"` 把容器 port 映射到宿主機；正式環境若只給同網路的應用用，可以不映射

### 5.6 日誌與健康檢查

```conf
logfile ""              # 空字串 = stdout → docker logs 看得到
```

```yaml
healthcheck:
  test: ["CMD", "redis-cli", "ping"]      # 有密碼：["CMD", "redis-cli", "-a", "密碼", "--no-auth-warning", "ping"]
  interval: 10s
  timeout: 5s
  retries: 5
restart: unless-stopped
```

`depends_on: condition: service_healthy` 讓 RedisInsight / exporter 等 Redis 真的能回 PONG 再啟動。

---

## 6. 監控：redis_exporter → Prometheus → Grafana

```
Redis :6379 ──INFO──▶ redis_exporter :9121 ──scrape──▶ Prometheus :9090 ──query──▶ Grafana :3000
```

- `docker/prometheus/prometheus.yml`：每 15 秒抓 exporter；另有 `spring-boot` job 抓 `host.docker.internal:8080/actuator/prometheus`（Linux 需要 compose 裡的 `extra_hosts: host-gateway`，已加）
- `docker/grafana/provisioning/`：自動建立 Prometheus 資料來源與 **Redis Tutorial** 儀表板（`docker/grafana/dashboards/redis-overview.json`）
- Grafana 帳密 `admin / admin` → Dashboards → Redis → Redis Tutorial

儀表板上的面板對應 [04 效能最佳化](04-performance-tuning.md) 的指標：ops/s、記憶體 vs maxmemory、命中率、淘汰數、各指令平均延遲、碎片率、複寫延遲、距上次 RDB 存檔秒數。

沒有 Grafana 也能直接看：

```bash
curl -s localhost:9121/metrics | grep -E "^redis_(up|connected_clients|memory_used_bytes|commands_processed_total)"
```

---

## 7. 常見錯誤

| 症狀 | 原因 | 處理 |
|---|---|---|
| `Bind for 0.0.0.0:6379 failed: port is already allocated` | 宿主機已有 Redis（另一套 compose、VM 安裝、brew） | `ss -tlnp \| grep 6379` 找出來停掉，或改映射 `"16379:6379"` |
| `Can't resolve instance hostname`（Sentinel 起不來） | `sentinel.conf` 用主機名稱但沒開 `resolve-hostnames`，或該容器還沒起來 | 本專案已改為固定 IP |
| Sentinel 一直 `+tilt`、不 failover | Master 名稱解析阻塞 | 同上，用 IP |
| `permission denied` 編輯 `docker/sentinel/sentinel.conf` | 舊版 compose 讓容器以 root 改寫了它 | `sudo chown $USER docker/sentinel/sentinel.conf` |
| `MISCONF Redis is configured to save RDB snapshots...` | `/data` 不可寫或磁碟滿 | 檢查 volume 權限、磁碟空間 |
| `WARNING Memory overcommit must be enabled` | 宿主機核心參數 | 5.4 節；練習可忽略 |
| Cluster：宿主機 `redis-cli -c` 被導到 `redis-node-2:7002` 連不上 | 宿主機解析不到容器名稱 | 在容器內操作，或加 `/etc/hosts` |
| `(error) CROSSSLOT Keys in request don't hash to the same slot` | Cluster 多 key 指令跨 slot | 用 hash tag `{...}` |
| Testcontainers 起不來 | Docker socket 權限 / Docker 沒跑 | `docker ps` 先確認；README 常見問題 |

---

## 8. 驗證

三套環境各有一支驗證腳本，`scripts/verify-all.sh` 會依序啟動、驗證、關閉：

```bash
./scripts/verify-all.sh          # 約 3–4 分鐘；需要 6379、6380–6381、26379–26381、7001–7006 空著
```

| 腳本 | 驗證項目 |
|---|---|
| `verify-single.sh` | 容器 healthy、`redis.conf` 每個關鍵設定生效、模組已載入（JSON.SET）、**重啟容器後資料仍在**、RedisInsight / exporter / Prometheus 端點 |
| `verify-sentinel.sh` | 6 個容器都在、Sentinel 互相發現、Replica 同步且唯讀、**停掉 Master → 幾秒內 `+switch-master`**、新 Master 有資料且可寫、**舊 Master 回來被降為 Replica** |
| `verify-cluster.sh` | `cluster_state:ok`、16384 slot 全指派、3 主 3 從、hash tag 同 slot、不帶 `-c` 收到 MOVED、`CROSSSLOT` 行為、**停掉一個 Master → Replica 接手、資料仍在、回歸後變 Replica** |

實測結果記錄在 [README 的驗證章節](../README.md#驗證方式與實測紀錄)。

---

## 9. 容器還是 VM？

| 面向 | 容器 | VM（[03](03-deploy-vm.md)） |
|---|---|---|
| 適合 | 學習、開發、CI、Kubernetes 上的正式環境 | 傳統正式環境、效能基準、需要完整 OS 調校 |
| 行程管理 | Docker / K8s 的 restart policy、探針 | systemd（graceful shutdown、資源限制、日誌） |
| OS 調校 | 受宿主機限制（overcommit、THP 改不了） | sysctl / THP / ulimit 全部可控 |
| 資料 | volume（掛錯就丟） | 直接在 VM 磁碟 |
| 記憶體 | cgroup limit 是硬牆，OOM 沒有日誌 | 整台機器的記憶體，OOM killer 有日誌 |
| 升級 | 換 image tag 重啟 | 換 binary / 套件重啟 |

兩者的 `redis.conf` 幾乎一樣——差別在誰負責「行程活著」和「OS 參數」。
