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
├── atlas.hcl               # Atlas 配置（dev/prod；连接由 migrate.sh 注入）
├── schema.sql              # 期望 schema 状态（唯一真实来源）
├── migrate.sh              # 同步脚本（本地 / CI 共用）
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
export DB_URL="maria://user:pass@host:3306/dbname"
./db/migrate.sh prod apply
```

不必设 `DEV_DB_URL`（脚本会建 `{dbname}_atlas`）。`apply` 使用 `--auto-approve`，不会交互提示。账号没有 `CREATE DATABASE` 时再显式设置。

每次 `apply` 在结构同步成功后都会幂等导入 `seed/base_data.sql`。部署所需的管理端账号和后续基础配置统一维护在此文件中；当前为 `root / 123456`。如需新增随部署写入数据库的默认配置，必须追加到 `seed/base_data.sql`，并保证可重复执行且不覆盖已有业务数据。已有同名账号不会被覆盖；首次登录后应立即修改默认密码。

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

### 应用层业务校验

平台角色和组织角色由应用枚举、写入入口及鉴权入口校验，数据库不设置角色 CHECK 或业务外键。未知角色不会获得权限；历史非法值应人工审核修正，不能自动升级为管理员。

如果测试或预发布库曾应用本 PR 的旧版角色 CHECK，先核对 `SHOW CREATE TABLE admin` 和 `SHOW CREATE TABLE organization_member`。确认约束存在后执行以下清理，再运行 `./db/migrate.sh dev apply`（生产环境仍遵循人工变更审核流程）：

```sql
ALTER TABLE admin DROP CONSTRAINT ck_admin_role;
ALTER TABLE organization_member DROP CONSTRAINT ck_org_member_role;
```

MariaDB 10.11 使用 `DROP CONSTRAINT` 删除 CHECK；当前 Atlas 版本生成的 `DROP CHECK` 不兼容此版本。此清理仅针对旧版新增约束，原有主键和账号唯一索引保留；成员索引调整见组织治理发布流程。

- `schema.sql` 是唯一真实来源，所有表结构变更必须通过修改此文件完成
- `dev` 没有删除保护，执行前确认目标库可安全重建
- `prod` 的 `diff.skip` 会阻止删除 Schema、表、字段、索引和外键；不要绕过 `atlas.hcl` 直接执行裸 Atlas 命令
- 字段类型等非删除变更仍可能影响数据，生产环境始终先执行 `prod plan`
- 种子数据（`seed/`）不纳入 schema 管理，仅用于开发环境初始化
- 每次变更前建议先 `git pull` 获取最新的 `schema.sql`

## Issue #169：注册申请快照切换

申请的用户引用、类型和密码快照一致性由应用层维护，不新增数据库 CHECK。审批通过前校验注册快照或已有账号，审批终结时清空密码哈希；拒绝申请可清理不完整快照。曾部署旧版申请 CHECK 的库，按前述审核流程确认约束存在后清理：

```sql
ALTER TABLE group_application DROP CONSTRAINT ck_application_password;
ALTER TABLE group_application DROP CONSTRAINT ck_application_user;
ALTER TABLE group_application DROP CONSTRAINT ck_application_snapshot;
```

新结构兼容旧的待审 user_id，但新代码审批要求 REGISTER 快照 user_id 为空且 password_hash 非空。
必须暂停注册、审批和组织治理，转换后才启动新实例。

1. 审核 prod plan，先排查重复待审邮箱和无效字段组合，再应用结构。
2. 导出所有待审/拒绝 REGISTER 申请及关联用户，人工确认仅为注册申请创建的占位账号。
3. 必须核对账号没有任何成员（包含已删除成员）、组织创建、任务、文件、操作记录等其他业务引用。
   DISABLED 和 default_org_id=0 不能独立作为删除依据。
4. 同一事务、同一条更新中复制待审占位用户的密码哈希及账号信息到申请快照，并清空申请 user_id。
   拒绝申请清空 user_id 和密码；已通过申请和 EXISTING_USER 不转换。
5. 只物理删除经审核、无其他业务引用的占位账号。核对影响行数及身份唯一键释放结果。
6. 待审 REGISTER 无遗留 user_id、密码非空，终态无密码，正式用户完整时才能 COMMIT，
   部署新代码并恢复写入。任何异常应 ROLLBACK 并继续停写。

结构唯一来源仍为 schema.sql，以上是经审核的数据处理流程，不新增结构迁移脚本。
待审核申请不独占全局身份。审批时身份已被占用则保持 PENDING；
需要继续加入时拒绝该注册申请，由对应正式账号提交 EXISTING_USER 申请。

## Issue #169：组织治理发布

所有权仅由未删除的 ORG_OWNER 成员表达，creator_id 保持审计含义。
所有者数量、启用状态及成员唯一性由应用层校验，不设置业务 CHECK、唯一索引或外键。
idx_org_member_user 和 idx_org_member_role 仅加速查询；组织写事务先锁组织行，审批在锁内检查有效成员，转让和治理验证唯一启用所有者。
生产 Atlas 保护索引和字段删除，旧约束及计算列不会因 schema.sql 移除而全部自动消失。

发布顺序：

1. 暂停组织创建、申请提交、审批、成员治理，停止旧实例写入。
2. 审核 prod plan 并同步结构，确认 idx_org_member_user、idx_org_member_role 查询索引已生效。
3. 核对 SHOW INDEX FROM organization_member 的结果，再经审核执行
   ALTER TABLE organization_member DROP INDEX uk_org_member_user。
   未实际删除旧索引不得开放移除/退出与重新入组，否则旧历史记录仍会占用成员唯一键。
   若库曾部署本 PR 的旧版，还需确认并清理 uk_org_member_owner、uk_org_member_active、
   ck_org_member_owner_enabled，以及仅为这些约束添加的 owner_org_id、active_guard 计算列。
   每项仅在 SHOW CREATE TABLE 确认存在后执行：

   ```sql
   ALTER TABLE organization_member DROP CONSTRAINT ck_org_member_owner_enabled;
   ALTER TABLE organization_member DROP INDEX uk_org_member_owner;
   ALTER TABLE organization_member DROP INDEX uk_org_member_active;
   ALTER TABLE organization_member DROP COLUMN owner_org_id, DROP COLUMN active_guard;
   ```
4. 同一事务回填所有未删除组织，包括被平台禁用的组织：
   优先选择仍全局启用、成员启用且未删除的创建者，设该成员为 ORG_OWNER；
   创建者不可用时，仅自动选择恰好一名全局及成员均启用的 ORG_ADMIN；
   多候选、无候选或非法角色进入业务负责人审核清单，不任意挑选。
5. 校验没有重复的有效成员（同组织、同用户、deleted_at IS NULL），
   且所有未删除组织恰好一名未删除、成员启用、用户存在且全局启用的 ORG_OWNER，
   否则回滚回填并继续停写。creator_id 不随转让变更。
6. 部署完整新版本后恢复写入，验证新建组织、转让、管理员边界与退出重新入组。

所有者核对查询必须返回零行：

    SELECT o.id
    FROM organization o
    LEFT JOIN organization_member m
      ON m.org_id=o.id AND m.deleted_at IS NULL AND m.org_role='ORG_OWNER'
    LEFT JOIN user u ON u.id=m.user_id AND u.deleted_at IS NULL
    WHERE o.deleted_at IS NULL
    GROUP BY o.id
    HAVING COUNT(m.id) <> 1
       OR SUM(CASE WHEN m.status=1 AND u.status=1 THEN 1 ELSE 0 END) <> 1;

有效成员核对查询必须返回零行：

    SELECT org_id, user_id
    FROM organization_member
    WHERE deleted_at IS NULL
    GROUP BY org_id, user_id
    HAVING COUNT(*) > 1;

新开发种子创建 ORG_OWNER；已有开发种子因 INSERT IGNORE 不会覆盖成员角色，应遵循同一回填流程。
解散是 organization.deleted_at 的逻辑删除，保留成员、任务和资源历史，拒绝所有待审申请并清空密码快照。
平台禁用可重新启用，解散不能通过启用接口恢复。已经发放的外部 COS 签名地址按原有效期失效；
本地签名资源入口会重新检查组织状态。

回填后不得回滚到不认识 ORG_OWNER 的旧实例；产生重复入组历史后不得恢复旧成员唯一索引。
需要回退时应停写并使用支持当前模型的兼容版本，优先向前修复。
