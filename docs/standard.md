# 项目规范

## 命名
对象、方法、文件等命名，可以写的详细，可读性优先

## Controller
1. 只做 DTO 的校验，BO 组装和 VO 的返回
2. 用户会话信息应该在 Controller 层取出并传入Service（对应 Service #2）

## Service 

1. Service 层入参和出参须使用 BO 类，入参：XXXXParamsBO,出参：XXXXResultBO
2. Service 层方法应保持环境无关性。凡涉及用户会话信息（如 userId、orgId），应通过方法参数显式传递，严禁在 Service 内部直接访问 ThreadLocal 或 Session 上下文。此举旨在确保业务逻辑在 Controller、定时任务 (Cron)、消息处理 (MQ) 及单元测试 等多场景下的高度可复用性。
3. 以 MySQL 作为唯一可靠数据源，Redis 仅做性能缓存加速，查询时优先走 Redis，一旦缓存查询异常、超时或宕机则自动降级直查 MySQL，保证业务不受缓存故障影响。

## Mapper
1. 数据库操作调用 Mapper 方法操作（参考https://baomidou.com/guides/data-interface/#mapper-interface），以 Lambda 操作优先，以保证类型安全

## 定时任务
1. 任务须实现 `ScheduledTask` 接口，通过 `getDefinition()` 返回任务定义
2. 任务定义使用 `ScheduledTaskDefinition` 构造，包含：taskName（唯一标识）、description、cronExpression（Spring 6字段格式）、enabled
3. 所有任务执行记录自动写入 `scheduled_task_log` 表，可通过 `admin/ScheduledTaskController` 手动触发任务
4. 定时任务应保持环境无关性 —— 业务逻辑中涉及用户会话信息（如 userId、orgId），须通过 `execute(LocalDateTime executeTime)` 方法参数传递，禁止在任务内部直接访问 ThreadLocal
5. 明确禁止在任务中获取当前时间，必须使用入参传递的 executeTime 作为当前时间(事实上定时任务传递的 executeTime 也确实是当前时间)，因为手动调用接口时可以传递任意的执行时间
6. 定时任务尽量保证幂等
