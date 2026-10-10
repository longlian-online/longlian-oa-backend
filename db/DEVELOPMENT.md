# Atlas 声明式数据库管理

## 概述

本项目使用 [Atlas](https://atlasgo.io/) 以**声明式（Declarative）**模式管理数据库结构。

`schema.sql` 是数据库结构的**唯一真实来源**，Atlas 自动计算当前数据库与期望状态的差异并应用变更。

### 为什么选择声明式

- 只维护一份目标结构 `schema.sql`，不维护版本式结构迁移链
- 修改表结构 = 编辑 `schema.sql` + 在开发环境执行 `./db/migrate.sh dev apply`
- Atlas 自动计算差异，避免手写 ALTER 语句
- 自动同步目标结构，旧字段保留到数据转换完成后再显式清理
- 轻量级：单二进制 / 小体积 Docker 镜像，无需 JVM

## 目录结构

```
db/
├── DEVELOPMENT.md          # 本文件
├── .gitignore              # 忽略本地配置
├── atlas.hcl               # Atlas 配置（dev/prod；连接由 migrate.sh 注入）
├── schema.sql              # 期望 schema 状态（唯一真实来源）
├── migrate.sh              # 同步脚本（本地 / CI 共用）
├── data-migrations/        # 按文件名记录成功状态的历史数据转换
├── operations/            # 经审核手动执行的运维 SQL，不自动扫描
├── tests/                 # 真实 MariaDB + Atlas 迁移集成验证
├── seed/                   # 初始化数据
│   ├── base_data.sql       # 每次 apply 均幂等导入的部署基础数据
│   └── dev_data.sql        # 仅本地开发的用户端与组织种子
```

## 配置连接

`migrate.sh` 是唯一入口。目标库二选一：

```bash
export DB_URL="maria://root:pass@localhost:3306/longlian_oa"
# 或把 Spring YAML 挂到 /app/config/application.yml（compose 默认如此）
./db/migrate.sh dev apply
```

`DEV_DB_URL` 可选。不设时在同一 MariaDB 实例自动创建 `{业务库}_atlas`。

### 关于暂存库

Atlas 声明式模式需要一个「暂存库」来推导期望状态：它把 `schema.sql` 真的在 MariaDB 上执行一遍，再 inspect 结果，从而让数据库自己解析类型归一化、默认 collation、索引顺序等细节，同时顺带校验生成的 DDL 合法。

这个库会被**反复清空重建**，必须是专用空库，绝不能指向有真实数据的库。账号没有 `CREATE DATABASE` 时，预先建好 `{业务库}_atlas` 并设置 `DEV_DB_URL`。

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

开发环境不自动删除字段，以便在结构同步后读取旧数据；Schema、表、索引和外键仍允许删除，因此只能指向可安全重建的开发数据库。

### 生产变更

```bash
# 1. 必须先审核生产环境将执行的 SQL
./db/migrate.sh prod plan
# 2. 确认后再同步生产数据库
./db/migrate.sh prod apply
```

## 命令说明

| 命令 | 说明 |
|---|---|
| `./db/migrate.sh dev plan` | 预览开发环境变更，不改动数据库 |
| `./db/migrate.sh dev apply` | 同步开发数据库，保留旧字段，随后执行未完成的数据迁移 |
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

`apply --dry-run` 使用与结构同步相同的 Atlas 计算路径。`migrate.sh plan` 在结构 SQL 后展示未记录的数据文件，但不会执行文件或写成功记录。生产部署须同时审核结构变更及数据 SQL。

## CI 集成

```bash
export DB_URL="maria://user:pass@host:3306/dbname"
./db/migrate.sh prod apply
```

不必设 `DEV_DB_URL`（脚本会建 `{dbname}_atlas`）。`apply` 使用 `--auto-approve`，不会交互提示。账号没有 `CREATE DATABASE` 时再显式设置。

每次 `apply` 在结构同步成功后都会幂等导入 `seed/base_data.sql`。部署所需的管理端账号和后续基础配置统一维护在此文件中；当前为 `root / 123456`。如需新增随部署写入数据库的默认配置，必须追加到 `seed/base_data.sql`，并保证可重复执行且不覆盖已有业务数据。已有同名账号不会被覆盖；首次登录后应立即修改默认密码。

### 自动数据迁移

`apply` 的执行顺序为：Atlas 结构同步 → 数据 SQL → 部署基础数据 → 可选种子数据。所有环境均保留旧字段，因此数据 SQL 可以在结构同步后读取旧字段。现有部署链路在迁移成功后才启动应用；单独重启应用进程不会再次运行迁移。对同一目标库的部署必须串行执行。

记录表 `data_migration` 与其他表一起由 Atlas 从 `schema.sql` 创建，不需要单独引导；该表仅供迁移执行器使用，不生成应用业务 Entity/Mapper。

- 数据文件直接放在 `db/data-migrations/*.sql`，按 ASCII 文件名顺序执行；文件名区分大小写，只允许 ASCII 字母、数字、下划线、点和短横线。
- 每次 `apply` 查询记录表；已有记录则跳过，未记录则执行整个 SQL 文件，成功后写入 `filename` 和 `executed_at`。已执行文件不得修改或改名，后续修正新增文件。
- 数据修改与成功记录在同一事务中提交。文件失败立即终止部署，不写成功记录，也不继续后续文件。
- 数据文件只做数据转换，不包含 DDL、事务提交、执行记录写入或 MariaDB 客户端命令。表结构由 `schema.sql` 管理，旧字段清理作为独立、经审核的运维操作。
- 新库也执行并记录文件；只需在文件内判断来源旧字段是否存在，不需要判断或创建目标结构。

集成测试入口是 `db/tests/migrate_test.sh`，需在具备 MariaDB 客户端和 Atlas 的环境中设置 `MYSQL_HOST`、`MYSQL_TCP_PORT`、`MYSQL_USER`、`MYSQL_PWD` 和不含库名的 `DB_SERVER_URL`。测试只创建并清理本次专用数据库，覆盖字段保留、结构先于数据同步、重复跳过、失败回滚与恢复。

## API 测试建表

API 测试在测试数据库为空时会直接执行根目录的 `db/schema.sql` 建表。Atlas 声明式模式不再维护独立的 `manifest/migrate/*.sql` 版本化迁移文件，因此不要新增第二份测试建表脚本。

## 从 MySQL 迁移现有数据

MySQL 与 MariaDB 的数据目录格式不作为迁移接口，禁止把原 MySQL 的 `/var/lib/mysql` 数据卷直接挂载给 MariaDB。已有环境按以下顺序做逻辑迁移：

1. 完整备份 MySQL，并停止业务写入。
2. 启动空的 MariaDB 10.11 实例，使用 `./db/migrate.sh prod apply` 从 `schema.sql` 创建目标结构。
3. 使用 MySQL 自带的 `mysqldump --single-transaction --no-create-info` 导出业务数据。
4. 使用 MariaDB 的 `mariadb` 客户端导入数据。
5. 比对所有业务表行数、唯一键、JSON 字段与时间字段，并运行完整 API 测试。
6. 将应用 JDBC 地址切换为 `jdbc:mariadb://`；确认稳定前保留原 MySQL 只读实例用于回滚。

排序规则已从 MySQL 8 专用的 `utf8mb4_0900_ai_ci` 调整为 MariaDB 10.11 支持的 `utf8mb4_unicode_520_ci`。正式导入前必须检查唯一索引字段在新排序规则下是否产生等价值冲突。

## 注意事项

- `schema.sql` 是唯一真实来源，所有表结构变更必须通过修改此文件完成
- `dev` 只保护字段删除，其他对象仍可能删除，执行前确认目标库可安全重建
- `prod` 的 `diff.skip` 会阻止删除 Schema、表、字段、索引和外键；不要绕过 `atlas.hcl` 直接执行裸 Atlas 命令
- 字段类型等非删除变更仍可能影响数据，生产环境始终先执行 `prod plan`
- 种子数据（`seed/`）不纳入 schema 管理，仅用于开发环境初始化
- 每次变更前建议先 `git pull` 获取最新的 `schema.sql`
