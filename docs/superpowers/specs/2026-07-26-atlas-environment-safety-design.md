# Atlas 环境安全与 API 测试建表设计

## 目标

统一以 `db/schema.sql` 作为声明式数据库结构的真实来源，并按环境采用不同的删除策略：

- 开发环境可完整收敛到期望结构，包括删除废弃的 Schema、表、字段、索引和外键。
- 生产环境不得自动删除 Schema、表、字段、索引和外键。
- API 测试不再依赖已删除的 `manifest/migrate/*.sql`，而是从仓库当前的声明式结构初始化测试数据库。

## Atlas 环境

`db/atlas.hcl` 将定义两个环境：

- `dev`：使用 `db/schema.sql`，不设置 `diff.skip` 删除保护。
- `prod`：使用同一份结构来源，但跳过 `drop_schema`、`drop_table`、`drop_column`、`drop_index` 和 `drop_foreign_key`。

两个环境的目标数据库和 Atlas 暂存数据库地址仍分别从 `DB_URL`、`DEV_DB_URL` 读取。`DEV_DB_URL` 必须是可丢弃的专用数据库，因为 Atlas 会反复清空并重建它，以计算期望结构。

生产保护只阻止删除操作。新增对象和非删除类变更，例如新增字段、修改 Atlas 支持的字段定义，仍可由 Atlas 执行。生产变更必须先执行 `prod plan` 审核 SQL，再执行 `apply`。

## 迁移脚本接口

`db/migrate.sh` 将要求显式传入环境，动作参数可选：

```sh
./db/migrate.sh dev apply
./db/migrate.sh dev plan
./db/migrate.sh prod inspect
```

保留 `apply`、`plan`、`inspect` 三种动作；只有环境参数合法时，才允许省略动作并默认使用 `apply`。脚本将校验参数并输出当前环境，避免现有脚本每次执行都默认指向 `prod`。

## API 测试数据库初始化

Atlas 声明式模式没有版本化的应用迁移文件，唯一的结构来源是 `db/schema.sql`。

`DatabaseCleanupUtil` 将：

1. 验证测试数据库连接。
2. 若 `user` 表已存在，则跳过建表。
3. 从 Maven 工作目录逐级向上查找 `db/schema.sql`，兼容在仓库根目录或 `app/` 模块目录执行测试。
4. 通过 Spring SQL 脚本工具在测试数据库中执行该文件。
5. 保留每个 API 测试方法执行前清空全部表数据的隔离机制。

测试进程不直接调用 Atlas CLI。这样 `mvn -pl app test` 无需依赖本地 Atlas CLI 或 Docker 守护进程，同时仍会在 MySQL 上执行仓库中的唯一结构来源。

## 文档与注释

更新 `db/DEVELOPMENT.md`、Atlas 配置内注释和迁移脚本注释，说明：

- `dev`、`prod` 的显式命令格式。
- 两个环境允许和禁止的删除操作。
- 生产执行前必须先运行 `plan`。
- API 测试执行的是 `db/schema.sql`，不存在另一份独立的建表脚本。

更新 `docs/api_test_standard.md`，将已失效的 classpath 迁移目录说明改为 `db/schema.sql`。

## 异常处理与验证

- 缺失或非法环境参数时，脚本会在调用 Atlas 前失败。
- 找不到 `db/schema.sql` 时，测试初始化会携带实际搜索路径失败。
- 保留现有数据库连接重试和测试数据清理行为。
- 验证包括 Shell 语法检查、可用时对两个环境执行 Atlas dry-run，以及通过 Docker 测试环境运行 API 测试。
