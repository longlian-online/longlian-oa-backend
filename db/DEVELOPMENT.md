# Atlas 声明式数据库管理

## 概述

本项目使用 [Atlas](https://atlasgo.io/) 以**声明式（Declarative）**模式管理数据库结构。

`schema.sql` 是数据库结构的**唯一真实来源**，Atlas 自动计算当前数据库与期望状态的差异并应用变更。

### 为什么选择声明式

- 只维护一份 `schema.sql`，无需管理版本化迁移文件
- 修改表结构 = 编辑 `schema.sql` + 执行 `task db:apply`
- Atlas 自动计算差异，避免手写 ALTER 语句
- 多环境始终收敛到同一期望状态，无漂移
- 轻量级：单二进制 / 小体积 Docker 镜像，无需 JVM

## 目录结构

```
db/
├── DEVELOPMENT.md          # 本文件
├── .gitignore              # 忽略本地配置
├── atlas.hcl               # Atlas 配置（环境定义）
├── schema.sql              # 期望 schema 状态（唯一真实来源）
├── migrate.sh              # CI 同步脚本
├── seed/                   # 开发环境种子数据
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

这会启动 MySQL 和 Redis 容器，并执行 `db:apply` 同步数据库。

## 开发流程

### 修改表结构

```bash
# 1. 编辑 schema.sql，修改为期望的表结构
#    例如：添加字段、修改类型、添加索引等
# 2. 预览差异（可选）
task db:diff
# 3. 应用变更到本地数据库
task db:apply
# 4. 提交 schema.sql 到 Git
```

### 导入种子数据（仅开发环境）

```bash
task db:seed
```

### 重置数据库

```bash
# 清空数据库并重新同步
task db:clean
task db:apply
task db:seed
```

## Taskfile 任务说明

| 任务 | 说明 |
|---|---|
| `task db:apply` | 将 schema.sql 声明的状态同步到数据库 |
| `task db:update` | 等同于 apply（兼容旧命令） |
| `task db:seed` | 导入开发环境种子数据 |
| `task db:diff` | 预览 schema.sql 与数据库的差异（不执行） |
| `task db:inspect` | 查看数据库当前 schema 状态 |
| `task db:clean` | 清空数据库（仅开发环境） |

## CI 集成

提供 `migrate.sh` 脚本供 CI 流水线调用：

```bash
# 同步数据库
DB_URL="mysql://user:pass@host:3306/dbname" \
DEV_DB_URL="mysql://user:pass@host:3306/dev" \
./db/migrate.sh apply

# 预览差异
DB_URL="mysql://user:pass@host:3306/dbname" \
DEV_DB_URL="mysql://user:pass@host:3306/dev" \
./db/migrate.sh diff
```

脚本自动检测环境：优先使用本地 `atlas` CLI，否则回退到 Docker。

## 注意事项

- `schema.sql` 是唯一真实来源，所有表结构变更必须通过修改此文件完成
- `atlas.hcl` 中的默认连接信息仅用于本地开发，CI 通过环境变量注入
- `diff.skip` 配置了 `drop_schema = true` 和 `drop_table = true`，防止意外删除
- 种子数据（`seed/`）不纳入 schema 管理，仅用于开发环境初始化
- 每次变更前建议先 `git pull` 获取最新的 `schema.sql`
