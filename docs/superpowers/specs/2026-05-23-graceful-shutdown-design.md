# 优雅停机设计

## 概述

为 longlian-oa 后端服务实现基础级的优雅停机（Graceful Shutdown），确保应用在收到关闭信号（SIGTERM/SIGINT）时能有序释放资源，避免正在执行的定时任务被粗暴中断导致数据不一致。

## 设计范围

- `server.shutdown=graceful` + 超时配置
- `ScheduledTaskEngine` 实现 `SmartLifecycle`，停止定时任务调度并等待执行中的任务完成
- 相关的运行中任务跟踪与关闭日志

## 不变更的内容

- `@Async` 虚拟线程任务（daemon 线程，JVM 关闭时自然终止）
- Druid 连接池销毁（已有 `destroyMethod = "close"`）
- COS 客户端销毁（已有 `DisposableBean.destroy()`）
- Redis 连接工厂（Spring Boot 自动配置处理）
- 不新增 readiness/liveness 探针

## 关闭顺序

```
SIGTERM/SIGINT
  │
  ▼
Web 服务器停止接受新请求          ← server.shutdown=graceful
  │
  ▼
Tomcat 优雅关闭完成                ← WebServerGracefulShutdownLifecycle (phase≈MAX-1)
  │
  ▼
ScheduledTaskEngine.stop()         ← SmartLifecycle (phase=MAX-100)
  ├─ shutdown=true
  ├─ 跳过新触发的 cron 任务
  └─ join 等待运行中的任务完成
      └─ 超时后记录警告日志
  │
  ▼
Spring 销毁单例 Bean               ← @PreDestroy, DisposableBean
  ├─ DruidDataSource.close()
  ├─ OssStorageService.destroy()
  ├─ SimpleAsyncTaskScheduler.close()
  └─ RedisConnectionFactory 关闭
  │
  ▼
JVM 退出
```

## 详细设计

### 1. application.yml 配置

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

- `server.shutdown=graceful`：关闭时 Web 服务器停止接受新请求，等待进行中的 HTTP 请求完成
- `timeout-per-shutdown-phase=30s`：每个 SmartLifecycle phase 的超时时间

### 2. ScheduledTaskEngine → SmartLifecycle

**修改点：**

#### 新增字段

```java
private final AtomicBoolean running = new AtomicBoolean(false);
private volatile boolean shutdown = false;
private final ConcurrentHashMap<String, Thread> runningTasks = new ConcurrentHashMap<>();
```

#### 实现 SmartLifecycle

| 方法 | 行为 |
|------|------|
| `start()` | `running.set(true)` |
| `stop()` | `shutdown=true` → 遍历 `runningTasks` 对每个存活线程 `join(剩余超时)` → 未完成的任务记录 `log.warn` |
| `stop(Runnable callback)` | 调用 `stop()` 后执行 `callback.run()` |
| `isRunning()` | 返回 `running.get()` |
| `getPhase()` | 返回 `Integer.MAX_VALUE - 100` |
| `isAutoStartup()` | 返回 `true` |

#### 跟踪运行中任务

在 `executeAndLog` 方法入口/出口：

```java
private void executeAndLog(...) {
    Thread currentThread = Thread.currentThread();
    runningTasks.put(taskName, currentThread);
    try {
        // 原有逻辑不变
    } finally {
        runningTasks.remove(taskName);
    }
}
```

#### 关闭时跳过新触发

Cron 调度的 lambda 包装：

```java
taskScheduler.schedule(
    () -> {
        if (shutdown) {
            log.info("系统正在关闭，跳过定时任务: {}", taskName);
            return;
        }
        executeAndLog(...);
    },
    new CronTrigger(cronExpression)
);
```

手动触发 API 入口：

```java
public void trigger(String taskName, LocalDateTime executeTime) {
    if (shutdown) {
        log.warn("系统正在关闭，拒绝手动触发任务: {}", taskName);
        return;
    }
    // 原有逻辑
}
```

### 3. 日志输出

关闭过程的关键日志：

| 事件 | 级别 | 消息 |
|------|------|------|
| 设置 shutdown 信号 | INFO | `开始优雅关闭，等待运行中的定时任务完成` |
| cron 任务被跳过 | INFO | `系统正在关闭，跳过定时任务: {taskName}` |
| 任务在超时内完成 | DEBUG | `定时任务在关闭前完成: {taskName}` |
| 任务超时未完成 | WARN | `定时任务未能在停机时间内完成: {taskName}` |
| 手动触发被拒绝 | WARN | `系统正在关闭，拒绝手动触发任务: {taskName}` |
| 关闭完成 | INFO | `优雅关闭完成，共等待 {n} 个运行中的任务` |

## 风险与注意事项

1. **虚拟线程 join**：虚拟线程的 `join()` 行为与平台线程一致，等待任务自然完成。不会中断任务，避免 `InterruptedException` 处理不当导致数据问题。
2. **Spring 版本兼容**：`SimpleAsyncTaskScheduler` 存在于 Spring 6.1+（Spring Boot 3.2+），当前项目使用 Spring Boot 3.3.5，兼容。
3. **重复触发边缘情况**：`shutdown=true` 设置后，已提交到 `SimpleAsyncTaskScheduler` 但尚未开始执行的任务，其 lambda 会在开始执行时检查 `shutdown` 然后跳过。理论上存在极短的时间窗口——但 `shutdown` 是 `volatile` 变量，保证可见性。
4. **@Async 任务**：不跟踪。虚拟线程为 daemon 线程，JVM 退出时自动终止。若未来需要跟踪，可引入 `AsyncTaskTracker` 组件统一管理。
