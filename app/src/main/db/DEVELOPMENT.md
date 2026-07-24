# Atlas 数据库版本管理

## 概述

本项目使用 [Atlas](https://atlasgo.io/) 作为数据库变更管理工具，采用**版本化迁移 + 声明式 Schema** 混合模式。

### 为什么使用 Atlas

- 原生纯 SQL 迁移文件，无特殊注释格式要求
- 声明式 Schema 管理：维护期望状态 `schema.sql`，自动生成差异迁移
- 内置迁移完整性校验（`atlas.sum`）和破坏性变更检测（`migrate lint`）
- 轻量级：单二进制 / 小体积 Docker 镜像，无需 JVM
- CI 友好：提供 `migrate.sh` 脚本供流水线调用

## 目录结构

```
app/src/main/db/
├── DEVELOPMENT.md          # 本文件
├── .gitignore              # 忽略本地配置
├── atlas.hcl               # Atlas 配置（环境定义）
├── schema.sql              # 期望 schema 状态（声明式源）
├── migrate.sh              # CI 迁移脚本
├── migrations/             # 版本化迁移文件
│   ├── 202606090000_baseline.sql
│   └── atlas.sum           # 校验和（自动生成，提交到 Git）
├── seed/                   # 开发环境种子数据（不纳入迁移）
│   └── dev_data.sql
└── Taskfile.yml            # task db:* 任务
```

## 前置条件

### 1. 安装 Docker

Atlas 以 Docker 方式运行（`arigaio/atlas`），无需本地安装 CLI。确保已安装 Docker Desktop。

### 2. 启动开发数据库

```bash
# 在项目根目录执行
task dev-prepare
```

这会启动 MySQL 和 Redis 容器，并执行 `db:update` 同步数据库。

## 开发流程

### 常规开发：修改 schema.sql + 生成 Diff

```bash
# 1. 编辑 schema.sql，添加/修改表结构（期望状态）
# 2. 生成差异迁移文件
task db:diff:add_invite_code_column
# 生成文件：migrations/20260725xxxxxx_add_invite_code_column.sql
# 3. 检查生成的 SQL 文件，确认变更正确
# 4. 应用迁移到本地数据库
task db:update
# 5. 提交到 Git（包括 migrations/ 和 schema.sql）
```

### 手动编写迁移（复杂变更时推荐）

在 `migrations/` 下创建 `YYYYMMDDHHMMSS_描述.sql` 文件，写入纯 SQL：

```sql
-- 添加邀请码字段
ALTER TABLE `user` ADD COLUMN invite_code VARCHAR(32) NULL COMMENT '邀请码';
CREATE INDEX idx_invite_code ON `user`(invite_code);
```

然后更新校验和：

```bash
task db:hash
```

并同步更新 `schema.sql` 使其反映最新期望状态。

### 应用迁移

```bash
task db:update
```

### 导入种子数据（仅开发环境）

```bash
task db:seed
```

## Taskfile 任务说明

| 任务 | 说明 |
|---|---|
| `task db:update` | 应用所有未执行的迁移到数据库 |
| `task db:seed` | 导入开发环境种子数据 |
| `task db:diff:描述` | 对比 schema.sql 与当前迁移状态，生成差异迁移文件 |
| `task db:lint` | 检查最新迁移文件的完整性与安全性 |
| `task db:hash` | 重新生成 atlas.sum 校验文件 |
| `task db:status` | 查看数据库迁移状态 |

## CI 集成

提供 `migrate.sh` 脚本供 CI 流水线调用：

```bash
# 应用迁移
DB_URL="mysql://user:pass@host:3306/dbname" ./app/src/main/db/migrate.sh apply

# 检查迁移文件
./app/src/main/db/migrate.sh lint

# 查看迁移状态
DB_URL="mysql://user:pass@host:3306/dbname" ./app/src/main/db/migrate.sh status
```

脚本自动检测环境：优先使用本地 `atlas` CLI，否则回退到 Docker。

## 注意事项

- `atlas.hcl` 中的默认连接信息仅用于本地开发，CI 通过环境变量注入
- 每次变更前建议先 `git pull` 获取最新的 migrations，避免冲突
- 禁止修改已提交的迁移文件 —— 如需修改应创建新的迁移
- 生成 diff 后务必 review SQL 文件，确认没有引入预期之外的变更
- `atlas.sum` 文件必须提交到 Git，它用于校验迁移文件完整性
- `schema.sql` 必须与 migrations 的最终状态保持一致
# Liquibase 数据库版本管理

## 概述

本项目使用 [Liquibase](https://www.liquibase.com/) 作为数据库变更管理工具，以 JSON 格式管理所有表结构变更。

### 为什么引入 Liquibase

此前数据库变更通过 `manifest/migrate/` 下的手写 SQL 文件管理，存在以下问题：

- 无法自动追踪哪些变更已应用到数据库
- 多环境（dev/sit/prod）间难以保持同步
- 缺乏回滚能力
- 新成员上手需要手动执行 SQL

Liquibase 通过变更集（changeset）唯一标识（id + author）自动追踪执行状态，确保每个变更只执行一次。

## 目录结构

```
app/src/main/db/
├── DEVELOPMENT.md              # 本文件
├── .gitignore                  # 忽略 liquibase.properties（含敏感信息）
├── liquibase.json              # Liquibase 依赖声明（mysql-connector）
├── liquibase.properties        # 数据库连接配置（本地开发库）
├── changelog.json              # 根 changelog，includeAll changelog/
├── changelog/
│   └── v1.0.0/                 # 按版本号组织变更集目录
│       └── 20260529__baseline.json # 基线变更集（全量库表结构）
├── liquibase_libs/
│   └── mysql-connector-java-8.0.30.jar  # JDBC 驱动（Docker 模式已内置）
└── Taskfile.yml                # db:update / db:diff:*:* 任务
```

## 前置条件

### 1. 安装 Docker

Liquibase 以 Docker 方式运行，无需本地安装 CLI。确保已安装 Docker Desktop。

### 2. 启动开发数据库

```bash
# 在项目根目录执行
task dev-prepare
```

这会启动 MySQL 和 Redis 容器，并执行 `db:update` 同步数据库。

或手动启动：

```bash
docker compose -f devops/docker-compose.dev.yml up -d
```

### 3. 配置连接信息

`liquibase.properties` 已配置本地开发库（`longlian_oa_dev`），已加入 `.gitignore`，各开发者可自行修改。

## 开发流程

### 常规开发：直接修改数据库 + 生成 Diff

```bash
# 1. 用 MySQL Workbench / Navicat / CLI 直接修改本地数据库
# 2. 生成差异变更集
task db:diff:版本号:描述
# 例如：task db:diff:v1.1.0:add_invite_code_column
# 生成文件：changelog/v1.1.0/20260529__add_invite_code_column.mysql.sql
# 3. 检查生成的 sql 文件，确认变更正确
# 4. 提交到 Git
```

### 手动创建变更集（复杂变更时推荐）

在 `changelog/{版本号}/` 下创建 `YYYYMMDD__描述.json` 文件，格式：

```json
{
    "databaseChangeLog": [
        {
            "changeSet": {
                "id": "唯一ID（可用时间戳）",
                "author": "your-name",
                "changes": [
                    {
                        "addColumn": {
                            "tableName": "table_name",
                            "columns": [
                                {
                                    "column": {
                                        "name": "new_column",
                                        "type": "VARCHAR(100)",
                                        "remarks": "字段说明"
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
        }
    ]
}
```

变更类型参考：[Liquibase JSON Format](https://docs.liquibase.com/concepts/changelogs/home.html)

### 应用变更

```bash
task db:update
```

或使用 Docker 直接执行：

```bash
docker run --rm -e INSTALL_MYSQL=true \
  -v $(pwd)/app/src/main/db:/liquibase/changelog \
  --network="host" \
  liquibase/liquibase \
  --defaults-file=/liquibase/changelog/liquibase.properties update
```

## Taskfile 任务说明

| 任务 | 说明 |
|---|---|
| `task db:update` | 应用所有未执行的变更集到数据库 |
| `task db:diff:版本号:描述` | 对比参考库与本地库，生成差异变更集 |

- `db:update` 在 `task dev-prepare` 中自动执行
- `db:diff:\*:\*` 的第一个 `*` 是版本号（如 `v1.1.0`），第二个 `*` 是差异描述（如 `add_field`），生成文件路径为 `changelog/{版本号}/{日期}__{描述}.json`
- Diff 对比的是 `liquibase.properties` 中配置的参考库（referenceUrl）和本地目标库（url）

### Docker 执行说明

Liquibase 通过 `liquibase/liquibase` 官方 Docker 镜像执行：

- `--network="host"` 使容器内的 Liquibase 可通过 localhost 连接宿主机上的 MySQL
- `INSTALL_MYSQL=true` 环境变量使容器内置 MySQL JDBC 驱动
- 宿主机 `db/` 目录挂载到容器的 `/liquibase/changelog`

## 注意事项

- `liquibase.properties` 包含数据库密码，已加入 `.gitignore`，**不要提交到 Git**
- 每次变更前建议先 `git pull` 获取最新的 changelog，避免 changeset ID 冲突
- 禁止修改已执行过的 changeset —— 如需修改应创建新的 changeset
- 生成 diff 后务必 review JSON 文件，确认没有引入预期之外的变更
