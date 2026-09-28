# K8s 部署配置（devops/k8s）

默认**全量部署**：MySQL 8.0 + Redis 7 + Atlas 迁移 Job + 后端 Deployment 全部跑在集群内，
入口走 Traefik。也可以退化为「仅后端」（复用外部 MySQL/Redis），见下文。

## 目录结构

| 文件 | 说明 |
|---|---|
| `namespace.yaml` | 命名空间 `longlian-oa` |
| `secret.yaml` | 部署配置 Secret（**占位值模板**，部署前必须替换）：应用配置 + MySQL 初始化脚本 + root 密码 |
| `mysql.yaml` | 集群内 MySQL 8.0（Deployment + PVC + Service，含应用账号/影子库初始化） |
| `redis.yaml` | 集群内 Redis 7（Deployment + PVC + Service） |
| `storage.yaml` | 应用本地文件存储 PVC（`storage.type=LOCAL` 时使用） |
| `migrate-job.yaml` | 数据库迁移 Job（Atlas 声明式同步，一次性） |
| `deployment.yaml` | 后端 Deployment（含探针、OTel、资源限制） |
| `service.yaml` | 后端集群内 Service（ClusterIP: 8080） |
| `ingress.yaml` | Traefik IngressRoute（含标准 Ingress 备选） |
| `kustomization.yaml` | kustomize 聚合入口 |

## 资源拓扑

```
Traefik (IngressRoute)
   │ https://oa.longlian.online
   ▼
longlian-oa Service ──▶ Deployment longlian-oa ──▶ PVC storage-pvc
                            │ 配置 /app/config/application.yml ← Secret longlian-oa-config
                            ├─▶ MySQL (mysql:3306)  ──▶ PVC mysql-data-pvc
                            └─▶ Redis (redis:6379)  ──▶ PVC redis-data-pvc
migrate Job（prod apply，读同一份配置，先于/伴随应用执行）
```

## 部署前置条件

1. 可用的 k8s 集群与 `kubectl`（命名空间内资源 + Secret 权限）。
2. 镜像已发布：`ghcr.io/longlian-online/longlian-oa-backend`
   （应用 `:latest` / `:1.2.3`；迁移 `:migration-latest` / `:1.2.3-migration`，
   由 `.github/workflows/publish-docker.yml` 在打 tag 时推送）。
3. （可选）Traefik（含 CRD）用于对外暴露；cert-manager 用于备选 Ingress 方案。
4. 默认全量部署**不需要**任何外部 MySQL / Redis。

## 快速开始（首次部署，默认全量部署）

### 1. 配置 Secret（唯一需要手工填真实值的地方）

编辑 `devops/k8s/secret.yaml`，替换所有 `CHANGE_ME_*` 占位值：

| 占位值 | 说明 |
|---|---|
| `CHANGE_ME_DB_PASSWORD` | 应用数据库账号 `longlian` 的密码，**两处保持一致**：`application-prod.yml` 的 `spring.datasource.password` 与 `mysql-init.sql` 的 `IDENTIFIED BY` |
| `CHANGE_ME_MYSQL_ROOT_PASSWORD` | MySQL root 密码（仅用于 `kubectl exec` 管理/备份） |
| `CHANGE_ME_JWT_SECRET_...` | ≥32 字节高强度随机串 |
| 邮件 `host/username/password` | 若不发邮件，可把 `notify.type` 改为 `NOOP` |
| `cors.allowed-origins` | 前端实际 Origin（默认 `https://oa.longlian.online`，与 Ingress 域名一致） |

### 2. 部署全部资源

```bash
kubectl apply -k devops/k8s
```

### 3. 等待数据库迁移完成

```bash
kubectl wait --for=condition=complete job/migrate -n longlian-oa --timeout=300s
kubectl logs job/migrate -n longlian-oa
```

> 迁移 Job 与后端 Deployment 同时调度。首次部署若应用先于迁移启动，会短暂
> CrashLoopBackOff——预期行为，MySQL 就绪且迁移完成后自动恢复（无需干预）。
> MySQL 首次初始化（建库/建账号）约需 30~60 秒，迁移 Job 有自动重试兜底。

### 4. 等待应用就绪并验证

```bash
kubectl rollout status deployment/longlian-oa -n longlian-oa --timeout=300s
kubectl get pods -n longlian-oa -w

# 本地联调验证
kubectl port-forward svc/longlian-oa 8080:8080 -n longlian-oa
curl -i -X POST http://localhost:8080/admin/session \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"..."}'
```

## 对外暴露（Traefik）

`ingress.yaml` 默认为 Traefik 原生 `IngressRoute`：

- 需集群已部署 Traefik（含 `traefik.io/v1alpha1` CRD）；未部署时该资源不会生效
  （apply 会报 CRD 不存在，可先删除 `ingress.yaml` 或稍后部署 Traefik 再 apply）。
- `tls.certResolver: letsencrypt` 需与 Traefik 静态配置对应（HTTP-01 challenge 要求
  域名已解析到集群入口且公网可达）；没配 ACME 时删除 `tls` 段即走 HTTP。
- 不用 Traefik 时，文件末尾有注释好的标准 `Ingress`（`ingressClassName: traefik` +
  cert-manager）备选，与 IngressRoute 二选一。
- 应用配置 `cors.allowed-origins` 必须包含实际访问域名。

## 升级流程（发布新版本）

### 无 schema 变更：只滚动更新应用

```bash
kubectl set image deployment/longlian-oa \
  longlian-oa=ghcr.io/longlian-online/longlian-oa-backend:1.2.3 -n longlian-oa
kubectl rollout status deployment/longlian-oa -n longlian-oa
```

### 有 schema 变更：先重跑迁移 Job，再滚动应用

```bash
# 1. 删除旧 Job 并更新迁移镜像 tag（编辑 migrate-job.yaml 中 image 为 :1.2.3-migration）
kubectl delete job migrate -n longlian-oa --ignore-not-found
kubectl apply -k devops/k8s

# 2. 等迁移成功后再滚动应用
kubectl wait --for=condition=complete job/migrate -n longlian-oa --timeout=300s
kubectl set image deployment/longlian-oa \
  longlian-oa=ghcr.io/longlian-online/longlian-oa-backend:1.2.3 -n longlian-oa
```

> 生产迁移是声明式同步，`atlas.hcl` 的 `prod` 环境跳过删除表/字段等危险操作；
> 发布前建议先看迁移将要执行的 SQL（本地 `./db/migrate.sh prod plan`）。

### 回滚

```bash
kubectl rollout undo deployment/longlian-oa -n longlian-oa
```

## 修改配置（JWT 密钥 / 数据库密码等）

1. 编辑 `devops/k8s/secret.yaml` 替换对应值。
   - 应用配置变更：重新 apply 后 `kubectl rollout restart deployment/longlian-oa -n longlian-oa`
     （subPath 挂载的 Secret 不会热更新，必须重启）。
   - **数据库密码变更**：`application-prod.yml` 与 `mysql-init.sql` 两处都要改；
     `mysql-init.sql` 只在 MySQL 数据卷为空时执行，改密码需额外手动执行
     `ALTER USER 'longlian'@'%' IDENTIFIED BY '...'`（见 FAQ）。
2. 迁移 Job 只在新部署时执行；仅改应用配置（不涉及库连接）无需重跑迁移。

## 存储与副本

- **MySQL / Redis**：单实例 Deployment + RWO PVC（DB 双写风险，禁止直接改多副本滚动）。
  负载评估后可用 StatefulSet + 主从/哨兵方案替换，属于数据库架构变更，需单独评审。
- **后端**：默认 `replicas: 1`（`storage.type=LOCAL` 配合 RWO PVC 只能挂单个节点）。
  多副本改造二选一：
  1. 存储改 OSS / COS（推荐）：修改 Secret 配置并删除 `storage.yaml` 与
     `deployment.yaml` 的 storage 卷，然后上调 `replicas`；
  2. PVC 改 ReadWriteMany（NFS / 云厂商 RWX），再上调 `replicas`。
- 多副本前提：`longlian.cache.type=redis`（集群内 Redis 已满足）。

## 仅部署后端（复用外部 MySQL / Redis）

已有外部实例时，把集群内组件去掉：

1. 删除 `kustomization.yaml` 中 `mysql.yaml`、`redis.yaml` 两条，并删除这两个文件（可选）。
2. 按 compose 的方式生成 Secret（会覆盖本仓库 Secret，移除 mysql-init / root 密码 key）：
   ```bash
   cp devops/application-prod.yml.example devops/application-prod.yml
   # 编辑 devops/application-prod.yml 填写外部 MySQL / Redis / 邮件地址与密钥
   kubectl create secret generic longlian-oa-config \
     --from-file=application-prod.yml=devops/application-prod.yml \
     --namespace longlian-oa --dry-run=client -o yaml | kubectl apply -f -
   ```
3. 迁移 Job 需要外部 MySQL 账号具备 `CREATE DATABASE` 权限（影子库 `{db}_atlas`
   自动创建），或预建影子库后在 `migrate-job.yaml` 设置 `DEV_DB_URL`。

## 数据库日常运维

```bash
# 进入 MySQL 管理
kubectl exec -it deploy/mysql -n longlian-oa -- mysql -uroot -p"$MYSQL_ROOT_PASSWORD"

# 逻辑备份（建议定期执行并另存）
kubectl exec -it deploy/mysql -n longlian-oa -- \
  mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction longlian_oa > backup.sql

# 查看 atlas 影子库状态
kubectl exec -it deploy/mysql -n longlian-oa -- \
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES;"
```

## 可观测性（OpenTelemetry）

发布镜像内置 OTel Java Agent（构建参数 `INCLUDE_OTEL=true`），Deployment 已通过
`JAVA_TOOL_OPTIONS` + `OTEL_*` 环境变量启用：

- 按集群实际 collector 地址修改 `OTEL_EXPORTER_OTLP_ENDPOINT`。
- 采样率 `OTEL_TRACES_SAMPLER_ARG: 0.5` 可按需调整。
- 不需要链路追踪时，删除 `JAVA_TOOL_OPTIONS` 与 `OTEL_*` 整段环境变量即可
  （使用不含 agent 的镜像时 Dockerfile 会自动剔除 `-javaagent` 参数，不会启动失败）。

## 常见问题

| 问题 | 处理 |
|---|---|
| 迁移 Job 失败 | `kubectl logs job/migrate -n longlian-oa` 查看。全量部署时常见原因：MySQL 尚未就绪（等 Job 自动重试即可）；外部模式则需给账号 `CREATE DATABASE` 权限或预建影子库 + `DEV_DB_URL` |
| 应用 CrashLoopBackOff | `kubectl logs deploy/longlian-oa -n longlian-oa` 查看。常见原因：Secret 仍是占位值、DB/Redis 密码不一致、邮箱未配置（`notify.type` 可临时改 `NOOP`） |
| 修改数据库密码不生效 | `mysql-init.sql` 只在数据卷首次初始化时执行；`kubectl exec` 进去手动 `ALTER USER 'longlian'@'%' IDENTIFIED BY '新密码'`，同时同步 Secret 两处 |
| MySQL 数据目录权限报错 | `mysql-data-pvc` 需集群默认 StorageClass 支持；fsGroup 999 已与镜像内 mysql 用户对齐，仍失败时检查 StorageClass 是否支持动态供给 |
| PVC `Pending` | 集群无默认 StorageClass；在对应 PVC 指定 `storageClassName` 或由管理员预建 PV |
| IngressRoute apply 报 CRD 不存在 | 集群未部署 Traefik；删除 `ingress.yaml` 或先部署 Traefik 再 apply |
| 为什么探针用 TCP 而不是 `/actuator/health` | `/actuator/**` 不在 `SecurityConstants` 免鉴权白名单，健康检查会返回 401；后续若放开可改 HTTP 探针 |
| 镜像拉取失败 | ghcr.io 公共镜像无需凭据；若为私有仓库，需要为 `longlian-oa` 命名空间配置 `imagePullSecret` 并加到 Deployment 的 `imagePullSecrets` |
| Redis 未设密码是否安全 | 仅集群网络可达；如需鉴权，Redis args 加 `--requirepass` 并从 Secret 注入，同时改 `application-prod.yml` 的密码 |
| 删除整个部署 | `kubectl delete -k devops/k8s`（所有 Secret、PVC 会一并删除，注意先备份 MySQL 数据与上传文件） |