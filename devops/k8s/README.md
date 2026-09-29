# K8s 部署配置（devops/k8s）

默认**全量部署**：MySQL 8.0 + Redis 7 + Atlas 迁移 Job + 后端 Deployment 全部跑在集群内，
入口走 Traefik。也可以退化为「仅后端」（复用外部 MySQL/Redis），见下文。

## 配置分离方式（核心）

仓库与服务器本地配置通过 **Kustomize Overlay** 分离，`git pull` 更新仓库配置时
**不会覆盖、不会冲突**服务器的本地真实配置：

```
devops/k8s/
├── base/                    # 仓库维护（随 git 更新）：通用部署结构，无任何真实值
│   ├── namespace.yaml
│   ├── storage.yaml
│   ├── mysql.yaml           # MySQL/Redis 等组件清单
│   ├── redis.yaml
│   ├── migrate-job.yaml     # 迁移 Job
│   ├── deployment.yaml      # 后端 Deployment
│   ├── service.yaml
│   └── ingress.yaml         # Traefik IngressRoute
└── overlays/
    └── prod/                # 服务器本地部署入口
        ├── kustomization.yaml        # 提交到仓库：引用 base + 声明本地文件
        ├── application-prod.yml      # ⚠️ 本地真实配置（.gitignore，从 .example 复制）
        ├── mysql-init.sql            # ⚠️ 本地真实初始化 SQL（.gitignore）
        ├── mysql-root-password       # ⚠️ 本地真实 root 密码（.gitignore）
        └── *.example                 # 模板，随仓库更新
```

部署命令固定为 `kubectl apply -k devops/k8s/overlays/prod`，每次执行时 kustomize
把「仓库的 base + 服务器本地的真实文件」合并渲染，再交给 kubectl：

```
仓库更新（git pull）  ──▶  base/*.yaml（结构/新组件/新参数）
服务器本地配置         ──▶  overlays/prod/application-prod.yml 等（真实值）
                            │
                            ▼
                    kustomize 合并 → kubectl apply
```

- 仓库将来改了 base（加组件、改探针、改镜像等）→ 服务器只需 `git pull`，本地配置自动叠加生效
- 本地真实值文件被 `.gitignore` 忽略 → 不会提交、不会被 pull 覆盖、也不会冲突
- 本地配置中的密码变更 → kustomize 生成的新 Secret 带新哈希名 → 应用自动滚动更新

## 目录结构（完整）

| 路径 | 说明 |
|---|---|
| `base/namespace.yaml` | 命名空间 `longlian-oa` |
| `base/mysql.yaml` | 集群内 MySQL 8.0（Deployment + PVC + Service，含应用账号/影子库初始化） |
| `base/redis.yaml` | 集群内 Redis 7（Deployment + PVC + Service） |
| `base/storage.yaml` | 应用本地文件存储 PVC（`storage.type=LOCAL` 时使用） |
| `base/migrate-job.yaml` | 数据库迁移 Job（Atlas 声明式同步，一次性） |
| `base/deployment.yaml` | 后端 Deployment（含探针、OTel、资源限制） |
| `base/service.yaml` | 后端集群内 Service（ClusterIP: 8080） |
| `base/ingress.yaml` | Traefik IngressRoute（含标准 Ingress 备选） |
| `overlays/prod/kustomization.yaml` | 部署入口：引用 base + 从本地文件生成 Secret |
| `overlays/prod/*.example` | 本地真实配置的模板（复制后填写） |

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
2. 镜像已发布：`docker.cnb.cool/longlian.online/longlian-oa-backend`
   （应用 `:latest` / `:1.2.3`；迁移 `:migration-latest` / `:1.2.3-migration`，
   由 `.github/workflows/publish-docker.yml` 在打 tag 时推送）。
3. 服务器有 git 访问权限（用于拉取仓库更新）。
4. （可选）Traefik（含 CRD）用于对外暴露；cert-manager 用于备选 Ingress 方案。
5. 默认全量部署**不需要**任何外部 MySQL / Redis。

## 快速开始（首次部署）

### 1. 拉取仓库并准备本地配置

```bash
git clone <repository-url> && cd longlian-oa-backend

# 复制模板为本地真实配置（这些文件已被 .gitignore 忽略）
cp devops/k8s/overlays/prod/application-prod.yml.example \
   devops/k8s/overlays/prod/application-prod.yml
cp devops/k8s/overlays/prod/mysql-init.sql.example \
   devops/k8s/overlays/prod/mysql-init.sql
cp devops/k8s/overlays/prod/mysql-root-password.example \
   devops/k8s/overlays/prod/mysql-root-password
```

### 2. 填写真实值（只需三个本地文件）

| 文件 | 要点 |
|---|---|
| `application-prod.yml` | 数据库/Redis 默认已指向集群内服务名；`jwt.secret` 换 >32 字节随机串；`spring.datasource.password` 与 `mysql-init.sql` 的 `IDENTIFIED BY` **保持一致** |
| `mysql-init.sql` | 把两处 `CHANGE_ME_DB_PASSWORD` 换成真实密码（与应用配置一致） |
| `mysql-root-password` | MySQL root 密码；**文件内容只含密码、末尾不要换行**（可直接用 `printf 'xxx' > mysql-root-password` 或 `openssl rand -base64 18 \| tr -d '\n' > mysql-root-password` 生成） |

> 邮件不配时把 `application-prod.yml` 的 `notify.type` 改为 `NOOP`；
> `cors.allowed-origins` 改为前端实际 Origin（默认与 Ingress 域名一致）。

### 3. 部署全部资源

```bash
kubectl apply -k devops/k8s/overlays/prod
```

### 4. 等待数据库迁移完成

```bash
kubectl wait --for=condition=complete job/migrate -n longlian-oa --timeout=300s
kubectl logs job/migrate -n longlian-oa
```

> 迁移 Job 与后端 Deployment 同时调度。首次部署若应用先于迁移启动，会短暂
> CrashLoopBackOff——预期行为，MySQL 就绪且迁移完成后自动恢复（无需干预）。
> MySQL 首次初始化（建库/建账号）约需 30~60 秒，迁移 Job 有自动重试兜底。

### 5. 等待应用就绪并验证

```bash
kubectl rollout status deployment/longlian-oa -n longlian-oa --timeout=300s
kubectl get pods -n longlian-oa -w

# 本地联调验证
kubectl port-forward svc/longlian-oa 8080:8080 -n longlian-oa
curl -i -X POST http://localhost:8080/admin/session \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"..."}'
```

## 日常更新（仓库配置更新时）

```bash
cd longlian-oa-backend
git pull                      # 拉取 base 更新，本地三个真实文件不受影响
kubectl apply -k devops/k8s/overlays/prod
```

## 升级流程（发布新版本）

### 无 schema 变更：只滚动更新应用

```bash
kubectl set image deployment/longlian-oa \
  longlian-oa=docker.cnb.cool/longlian.online/longlian-oa-backend:1.2.3 -n longlian-oa
kubectl rollout status deployment/longlian-oa -n longlian-oa
```

### 有 schema 变更：先重跑迁移 Job，再滚动应用

```bash
# 1. 删除旧 Job 并更新迁移镜像 tag（编辑 base/migrate-job.yaml 中 image 为 :1.2.3-migration）
kubectl delete job migrate -n longlian-oa --ignore-not-found
kubectl apply -k devops/k8s/overlays/prod

# 2. 等迁移成功后再滚动应用
kubectl wait --for=condition=complete job/migrate -n longlian-oa --timeout=300s
kubectl set image deployment/longlian-oa \
  longlian-oa=docker.cnb.cool/longlian.online/longlian-oa-backend:1.2.3 -n longlian-oa
```

> 生产迁移是声明式同步，`atlas.hcl` 的 `prod` 环境跳过删除表/字段等危险操作；
> 发布前建议先看迁移将要执行的 SQL（本地 `./db/migrate.sh prod plan`）。

### 回滚

```bash
kubectl rollout undo deployment/longlian-oa -n longlian-oa
```

## 修改本地配置（JWT 密钥 / 数据库密码等）

只改服务器本地的 `overlays/prod/*` 三个文件，然后：

```bash
kubectl apply -k devops/k8s/overlays/prod
```

Secret 名称带内容哈希：配置一变 → 新 Secret → 应用自动滚动更新，无需手动重启；
若改了 `spring.datasource`（库连接）且已在跑，需按上文重跑迁移 Job。

> **数据库密码变更**：`application-prod.yml` 与 `mysql-init.sql` 两处都要改；
> `mysql-init.sql` 只在 MySQL 数据卷首次初始化时执行，改密码需额外手动执行
> `ALTER USER 'longlian'@'%' IDENTIFIED BY '...'`（见 FAQ）。

## 对外暴露（Traefik）

`base/ingress.yaml` 默认为 Traefik 原生 `IngressRoute`：

- 需集群已部署 Traefik（含 `traefik.io/v1alpha1` CRD）；未部署时该资源不会生效
  （apply 会报 CRD 不存在，可先删除该文件引用或稍后部署 Traefik 再 apply）。
- `tls.certResolver: letsencrypt` 需与 Traefik 静态配置对应（HTTP-01 challenge 要求
  域名已解析到集群入口且公网可达）；没配 ACME 时删除 `tls` 段即走 HTTP。
- 不用 Traefik 时，文件末尾有注释好的标准 `Ingress`（`ingressClassName: traefik` +
  cert-manager）备选，与 IngressRoute 二选一。
- 应用配置 `cors.allowed-origins` 必须包含实际访问域名。

## 仅部署后端（复用外部 MySQL / Redis）

1. 注释掉 `overlays/prod/kustomization.yaml` 中 `mysql.yaml`、`redis.yaml` 两行引用。
2. 外部组件地址在本地 `overlays/prod/application-prod.yml` 中填写
   （`spring.datasource.url` / `spring.data.redis.host` 改为外部实例地址）。
3. 迁移 Job 需要外部 MySQL 账号具备 `CREATE DATABASE` 权限（影子库 `{db}_atlas`
   自动创建），或预建影子库后在 `base/migrate-job.yaml` 设置 `DEV_DB_URL`。

## 存储与副本

- **MySQL / Redis**：单实例 Deployment + RWO PVC（DB 双写风险，禁止直接改多副本滚动）。
  负载评估后可用 StatefulSet + 主从/哨兵方案替换，属于数据库架构变更，需单独评审。
- **后端**：默认 `replicas: 1`（`storage.type=LOCAL` 配合 RWO PVC 只能挂单个节点）。
  多副本改造二选一：
  1. 存储改 OSS / COS（推荐）：修改配置并删除 `base/storage.yaml` 与
     `base/deployment.yaml` 的 storage 卷引用，然后上调 `replicas`；
  2. PVC 改 ReadWriteMany（NFS / 云厂商 RWX），再上调 `replicas`。
- 多副本前提：`longlian.cache.type=redis`（集群内 Redis 已满足）。

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

- 按集群实际 collector 地址修改 `base/deployment.yaml` 的 `OTEL_EXPORTER_OTLP_ENDPOINT`。
- 采样率 `OTEL_TRACES_SAMPLER_ARG: 0.5` 可按需调整。
- 不需要链路追踪时，删除 `JAVA_TOOL_OPTIONS` 与 `OTEL_*` 整段环境变量即可
  （使用不含 agent 的镜像时 Dockerfile 会自动剔除 `-javaagent` 参数，不会启动失败）。

## 常见问题

| 问题 | 处理 |
|---|---|
| 迁移 Job 失败 | `kubectl logs job/migrate -n longlian-oa` 查看。全量部署时常见原因：MySQL 尚未就绪（等 Job 自动重试即可）；外部模式则需给账号 `CREATE DATABASE` 权限或预建影子库 + `DEV_DB_URL` |
| Job `spec.template` field is immutable | Job 模板不可变。改镜像或 securityContext 后必须先 `kubectl delete job migrate -n longlian-oa --ignore-not-found` 再 `kubectl apply -k devops/k8s/overlays/prod` |
| CreateContainerConfigError / runAsNonRoot | 镜像 USER 是名字 `appuser` 时 kubelet 无法校验非 root。Deployment 需 `runAsUser: 100`/`runAsGroup: 101`，migrate Job 需 `runAsUser: 998`/`runAsGroup: 998` |
| 应用 CrashLoopBackOff | `kubectl logs deploy/longlian-oa -n longlian-oa` 查看。常见原因：本地配置仍是占位值、DB/Redis 密码不一致、邮箱未配置（`notify.type` 可临时改 `NOOP`） |
| 修改数据库密码不生效 | `mysql-init.sql` 只在数据卷首次初始化时执行；`kubectl exec` 进去手动 `ALTER USER 'longlian'@'%' IDENTIFIED BY '新密码'`，同时同步本地两个文件 |
| MySQL root 密码带换行 | `mysql-root-password` 文件末尾不能有换行（kustomize 原样读入），用 `printf 'xxx' > mysql-root-password` 写入 |
| MySQL 数据目录权限报错 | `mysql-data-pvc` 需集群默认 StorageClass 支持；fsGroup 999 已与镜像内 mysql 用户对齐，仍失败时检查 StorageClass 是否支持动态供给 |
| PVC `Pending` | 集群无默认 StorageClass；在对应 PVC 指定 `storageClassName` 或由管理员预建 PV |
| IngressRoute apply 报 CRD 不存在 | 集群未部署 Traefik；注释掉 `overlays/prod/kustomization.yaml` 的 ingress 引用或先部署 Traefik 再 apply |
| 为什么探针用 TCP 而不是 `/actuator/health` | `/actuator/**` 不在 `SecurityConstants` 免鉴权白名单，健康检查会返回 401；后续若放开可改 HTTP 探针 |
| 镜像拉取失败 | docker.cnb.cool 私有制品库需凭据：为 `longlian-oa` 命名空间创建 `imagePullSecret`（`docker login docker.cnb.cool -u cnb -p $CNB_TOKEN`），并加到 Deployment / Job 的 `imagePullSecrets` |
| 旧的 Secret 残留 | kustomize 内容哈希会生成新 Secret 名，旧 Secret 不会自动删除：`kubectl get secrets -n longlian-oa` 确认后手动清理 |
| Redis 未设密码是否安全 | 仅集群网络可达；如需鉴权，Redis args 加 `--requirepass` 并从 Secret 注入，同时改应用配置的密码 |
| 删除整个部署 | `kubectl delete -k devops/k8s/overlays/prod`（所有 Secret、PVC 会一并删除，注意先备份 MySQL 数据与上传文件） |