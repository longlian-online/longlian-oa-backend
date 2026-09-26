# 项目规范

## 命名
对象、方法、文件等命名，可以写的详细，可读性优先

## 注释
1. 不在没有理由的前提下移除注释

## 最小实现决策梯

处理需求前，先完整理解需求与受影响代码路径；然后按顺序停在第一个满足条件的层级：

1. 需求是否真的需要新增实现？不需要则明确说明并不实现。
2. 项目中是否已有可复用的能力、模式或基础设施？优先复用。
3. JDK / Spring / MyBatis Plus 等已使用的标准能力是否已经覆盖？优先使用。
4. 运行平台或浏览器原生能力是否覆盖？优先使用。
5. 已安装依赖是否覆盖？优先使用，不新增依赖。
6. 是否能以直接、清晰的一小段代码实现？
7. 仅在上述方式都不成立时，新增最小且完整的实现。

## Controller
1. 只做 DTO 的校验，BO 组装和 VO 的返回
2. 用户会话信息应该在 Controller 层取出并传入Service（对应 Service #2）

## Service 

1. Service 层入参和出参须使用 BO 类，入参：XXXXParamsBO,出参：XXXXResultBO
2. Service 层方法应保持环境无关性。凡涉及用户会话信息（如 userId、orgId），应通过方法参数显式传递，严禁在 Service 内部直接访问 ThreadLocal 或 Session 上下文。此举旨在确保业务逻辑在 Controller、定时任务 (Cron)、消息处理 (MQ) 及单元测试 等多场景下的高度可复用性。
3. 以 MySQL 作为唯一可靠数据源，Redis 仅做性能缓存加速，查询时优先走 Redis，一旦缓存查询异常、超时或宕机则自动降级直查 MySQL，保证业务不受缓存故障影响。
4. 依赖事务提交后执行的缓存失效、通知发送等操作，调用链已明确保证事务同步时应直接注册 `afterCommit` 回调；禁止增加“无事务则立即执行”的兜底分支。缺少事务属于调用约束被破坏，应快速失败，避免产生与正常路径不同的提交、回滚语义。
5. 仅当组件的明确职责包含“既可加入现有事务，也可独立调用”时，才允许处理无事务场景；此时必须显式建立事务并测试两种调用路径。

## Mapper
1. 数据库操作调用 Mapper 方法操作（参考https://baomidou.com/guides/data-interface/#mapper-interface），以 Lambda 操作优先，以保证类型安全

## 数据库变更

1. 数据库使用 Atlas 声明式管理，`db/schema.sql` 是数据库结构的唯一真实来源。
2. 修改数据库结构时，直接修改 `db/schema.sql` 中对应的现有表定义，使其表达目标状态。
3. 禁止新增或维护版本式迁移目录、迁移文件或重复的建表脚本；不要在 `db/data-migrations/` 下编写结构变更 SQL。
4. 开发环境使用 `./db/migrate.sh dev plan` 预览变更，再使用 `./db/migrate.sh dev apply` 同步数据库；生产环境必须先审核 `./db/migrate.sh prod plan` 的结果。
5. 从 `schema.sql` 移除字段、表或其他对象只表达目标结构。生产环境的删除操作受保护，不会自动执行；涉及历史数据清理的变更必须作为单独、经审核的运维操作执行。

## 定时任务
1. 任务须实现 `ScheduledTask` 接口，通过 `getDefinition()` 返回任务定义
2. 任务定义使用 `ScheduledTaskDefinition` 构造，包含：taskName（唯一标识）、description、cronExpression（Spring 6字段格式）、enabled
3. 所有任务执行记录自动写入 `scheduled_task_log` 表，可通过 `admin/ScheduledTaskController` 手动触发任务
4. 定时任务应保持环境无关性 —— 业务逻辑中涉及用户会话信息（如 userId、orgId），须通过 `execute(LocalDateTime executeTime)` 方法参数传递，禁止在任务内部直接访问 ThreadLocal
5. 明确禁止在任务中获取当前时间，必须使用入参传递的 executeTime 作为当前时间(事实上定时任务传递的 executeTime 也确实是当前时间)，因为手动调用接口时可以传递任意的执行时间
6. 定时任务尽量保证幂等
