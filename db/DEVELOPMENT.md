# Atlas 声明式数据库管理

## 概述

本项目使用 [Atlas](https://atlasgo.io/) 以**声明式（Declarative）**模式管理数据库结构。

`schema.sql` 是数据库结构的**唯一真实来源**，Atlas 自动计算当前数据库与期望状态的差异并应用变更。

### 为什么选择声明式

- 只维护一份 `schema.sql`，无需管理版本化迁移文件
- 修改表结构 = 编辑 `schema.sql` + 在开发环境执行 `./db/migrate.sh dev apply`
- Atlas 自动计算差异，避免手写 ALTER 语句
- 多环境始终收敛到同一期望状态，无漂移
- 轻量级：单二进制 / 小体积 Docker 镜像，无需 JVM

## 目录结构

```
db/
├── DEVELOPMENT.md          # 本文件
├── .gitignore              # 忽略本地配置
├── atlas.hcl               # Atlas 配置（dev/prod 环境，连接从环境变量读取）
├── schema.sql              # 期望 schema 状态（唯一真实来源）
├── migrate.sh              # 同步脚本（本地 / CI 共用）
└── seed/                   # 开发环境种子数据
    └── dev_data.sql
```

## 配置连接

`atlas.hcl` 定义 `dev`、`prod` 两个环境，连接信息全部从环境变量读取，**不内置任何默认值**。使用前必须 export：

```bash
export DB_URL="postgresql://postgres:pass@localhost:5432/longlian_oa"
export DEV_DB_URL="postgresql://postgres:pass@localhost:5432/longlian_oa_diff"
```

未设置时 Atlas 会直接报错并提示缺哪个变量。

### 关于 DEV_DB_URL

Atlas 声明式模式需要一个「暂存库」来推导期望状态：它把 `schema.sql` 真的在 PostgreSQL 上执行一遍，再 inspect 结果，从而让数据库自己解析类型归一化、默认 collation、索引顺序等细节，同时顺带校验生成的 DDL 合法。

这个库会被**反复清空重建**，必须是专用空库。绝不能指向有真实数据的库 —— Atlas 检测到非空会拒绝工作并报 `connected database is not clean`。

## 前置条件

`migrate.sh` 优先使用本地 `atlas` CLI，没有则回退到 Docker 镜像 `arigaio/atlas`。两者装其一即可。

```bash
# 可选：安装本地 CLI（比 Docker 快）
curl -sSf https://atlasgo.sh | sh
```

## 开发流程

### 修改表结构

```bash
# 1. 编辑 schema.sql，改成期望的表结构
# 2. 预览将执行的 SQL（不改库）
./db/migrate.sh dev plan
# 3. 应用到开发数据库
./db/migrate.sh dev apply
# 4. 提交 schema.sql 到 Git
```

开发环境允许删除已从 `schema.sql` 移除的 Schema、表、字段、索引和外键，因此只能指向可安全重建的开发数据库。

### 生产变更

```bash
# 1. 必须先审核生产环境将执行的 SQL
./db/migrate.sh prod plan
# 2. 确认后再同步生产数据库
./db/migrate.sh prod apply
```

生产环境会跳过删除 Schema、表、字段、索引和外键的操作。即使对象已从 `schema.sql` 移除，它们也会继续保留在生产数据库中；需要删除时应走单独、经审核的人工变更流程。

## 命令说明

| 命令 | 说明 |
|---|---|
| `./db/migrate.sh dev plan` | 预览开发环境变更，不改动数据库 |
| `./db/migrate.sh dev apply` | 将开发数据库完整同步到 `schema.sql`，允许删除废弃对象 |
| `./db/migrate.sh prod plan` | 预览生产环境变更，不改动数据库 |
| `./db/migrate.sh prod apply` | 同步生产数据库，但不自动删除 Schema、表、字段、索引和外键 |
| `./db/migrate.sh <dev\|prod> inspect` | 查看指定环境的当前结构 |

底层等价命令（需自行 export 环境变量，且 `--env` 必须传）：

```bash
cd db
atlas schema apply --env dev --dry-run   # 对应开发环境 plan
atlas schema apply --env dev             # 对应开发环境 apply
atlas schema inspect --env prod           # 查看生产环境结构
```

### 为什么用 `apply --dry-run` 而不是 `schema diff`

`schema diff` 是通用的两端对比命令，`--from` / `--to` 必填，且不从 `--env` 继承端点。即使显式传 `--from env://url --to env://src`，由于 `schema.sql` 里没有 `CREATE DATABASE`，加载进暂存库后 schema 名与目标库不一致，Atlas 会报 `modify schema "" is not allowed when migration plan is scoped to one schema`。

`apply --dry-run` 没有这个问题，而且它展示的就是 `apply` 真正会执行的语句，不存在两条代码路径算出不同结果的可能。

## CI 集成

```bash
export DB_URL="postgresql://user:pass@host:5432/dbname"
export DEV_DB_URL="postgresql://user:pass@host:5432/dev"
./db/migrate.sh prod apply
```

`apply` 在脚本中使用 `--auto-approve`，不会交互提示。

## API 测试建表

API 测试在测试数据库为空时会直接执行根目录的 `db/schema.sql` 建表。Atlas 声明式模式不再维护独立的 `manifest/migrate/*.sql` 版本化迁移文件，因此不要新增第二份测试建表脚本。

## 注意事项

- `schema.sql` 是唯一真实来源，所有表结构变更必须通过修改此文件完成
- `dev` 没有删除保护，执行前确认 `DB_URL` 指向可安全重建的开发数据库
- `prod` 的 `diff.skip` 会阻止删除 Schema、表、字段、索引和外键；不要绕过 `atlas.hcl` 直接执行裸 Atlas 命令
- 字段类型等非删除变更仍可能影响数据，生产环境始终先执行 `prod plan`
- 种子数据（`seed/`）不纳入 schema 管理，仅用于开发环境初始化
- 每次变更前建议先 `git pull` 获取最新的 `schema.sql`
