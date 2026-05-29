package online.longlian.app.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.service.DistributedLockService;
import online.longlian.app.common.util.TraceIdUtil;
import online.longlian.app.pojo.bo.common.ScheduledTaskDefinition;
import online.longlian.app.service.scheduled.ScheduledTaskLogService;
import online.longlian.app.service.app.SessionService;
import online.longlian.common.enumeration.ScheduledTaskStatus;
import online.longlian.common.enumeration.TriggerSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务调度引擎。
 * <p>
 * 启动时扫描所有 {@link ScheduledTask} Bean，根据定义中的 cron 表达式自动注册调度；
 * 同时提供手动触发接口供 API 调用。
 * <p>
 * 内部通过 traceId（{@link TraceIdUtil}）、triggerSource、triggeredBy 记录元信息用于日志和排障，
 * 但这些信息不暴露给业务任务——业务任务只接收一个 {@code LocalDateTime executeTime} 参数。
 * <p>
 * 实现 {@link SmartLifecycle} 以支持优雅停机：
 * 关闭时停止调度新任务，等待运行中的任务完成（不中断），超时后记录告警日志。
 * <p>
 * TODO: 并发互斥 —— 同一 taskName 不应重复执行。后续需要加入 per-task 锁（ReentrantLock 或 Redis 分布式锁），
 *       当 taskName 已有任务在执行时，新触发（无论 cron 还是手动）应直接跳过。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskEngine implements SmartLifecycle {

    private final TaskScheduler taskScheduler;
    private final ApplicationContext applicationContext;
    private final SessionService sessionService;
    private final ScheduledTaskLogService taskLogService;
    private final DistributedLockService lockService;

    /**
     * 所有已注册的任务，按 taskName 索引
     */
    private final Map<String, ScheduledTask> taskMap = new ConcurrentHashMap<>();

    /**
     * SmartLifecycle 运行状态
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 停机超时时间，从 spring.lifecycle.timeout-per-shutdown-phase 注入，避免与配置不同步。
     */
    @Value("${spring.lifecycle.timeout-per-shutdown-phase}")
    private Duration shutdownTimeout;

    /**
     * 停机标志，设置后 cron 新触发和手动触发均被拒绝。
     * volatile 保证跨线程可见性。
     */
    private volatile boolean shutdown = false;

    /**
     * 当前正在执行的任务线程，按 taskName 索引，用于停机时 join 等待。
     */
    private final ConcurrentHashMap<String, Thread> runningTasks = new ConcurrentHashMap<>();

    // ==================== SmartLifecycle ====================

    @Override
    public void start() {
        running.set(true);
        Map<String, ScheduledTask> beans = applicationContext.getBeansOfType(ScheduledTask.class);
        for (ScheduledTask task : beans.values()) {
            ScheduledTaskDefinition def = task.getDefinition();
            taskMap.put(def.getTaskName(), task);
            if (def.isEnabled() && def.getCronExpression() != null && !def.getCronExpression().isBlank()) {
                scheduleCron(def.getTaskName(), def.getCronExpression());
            }
            log.info("注册定时任务: {} | cron={} | enabled={}",
                    def.getTaskName(), def.getCronExpression(), def.isEnabled());
        }
    }

    @Override
    public void stop() {
        log.info("开始优雅关闭，等待运行中的定时任务完成");
        shutdown = true;

        if (runningTasks.isEmpty()) {
            log.info("优雅关闭完成，无运行中的任务");
            running.set(false);
            return;
        }

        // 快照当前运行中的任务，避免迭代期间 ConcurrentHashMap 弱一致性导致遗漏或重复
        Map<String, Thread> snapshot = new HashMap<>(runningTasks);
        long deadline = System.currentTimeMillis() + shutdownTimeout.toMillis();

        for (Map.Entry<String, Thread> entry : snapshot.entrySet()) {
            String taskName = entry.getKey();
            Thread thread = entry.getValue();
            long remaining = deadline - System.currentTimeMillis();
            if (remaining > 0) {
                try {
                    thread.join(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("等待任务完成时被中断: {}", taskName);
                    break;
                }
            }
            if (thread.isAlive()) {
                log.warn("定时任务未能在停机时间内完成: {}", taskName);
            } else {
                log.info("定时任务在关闭前完成: {}", taskName);
            }
        }

        running.set(false);
        log.info("优雅关闭完成，共等待 {} 个运行中的任务", snapshot.size());
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Phase 设为 MAX_VALUE - 100，确保在 Tomcat 优雅关闭之后、Spring Bean 销毁之前执行。
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    // ==================== 对外接口 ====================

    public Map<String, ScheduledTask> getRegisteredTasks() {
        return Map.copyOf(taskMap);
    }

    public ScheduledTask getTask(String taskName) {
        ScheduledTask task = taskMap.get(taskName);
        if (task == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务不存在: " + taskName);
        }
        return task;
    }

    /**
     * 手动触发任务执行。
     *
     * @param taskName    任务名称
     * @param executeTime 上层传递的执行时间（为 null 时默认使用当前时间）
     */
    public void trigger(String taskName, LocalDateTime executeTime) {
        if (shutdown) {
            log.warn("系统正在关闭，拒绝手动触发任务: {}", taskName);
            return;
        }
        ScheduledTask task = getTask(taskName);
        LocalDateTime execTime = executeTime != null ? executeTime : LocalDateTime.now();
        Long userId = getCurrentUserIdSafely();

        executeAndLog(task, taskName, execTime, TriggerSource.MANUAL, userId);
    }

    // ==================== 内部实现 ====================

    private void scheduleCron(String taskName, @NonNull String cronExpression) {
        ScheduledTask task = taskMap.get(taskName);
        taskScheduler.schedule(
                () -> {
                    if (shutdown) {
                        log.info("系统正在关闭，跳过定时任务: {}", taskName);
                        return;
                    }
                    executeAndLog(task, taskName, LocalDateTime.now(), TriggerSource.SCHEDULED, null);
                },
                new CronTrigger(cronExpression)
        );
    }


    /**
     * 执行任务并记录日志，同时将执行线程注册到 runningTasks 以支持停机等待。
     */
    private void executeAndLog(ScheduledTask task, String taskName, LocalDateTime executeTime,
                                TriggerSource source, Long triggeredBy) {
        Thread currentThread = Thread.currentThread();
        runningTasks.put(taskName, currentThread);
        try {
            doExecuteAndLog(task, taskName, executeTime, source, triggeredBy);
        } finally {
            runningTasks.remove(taskName);
        }
    }

    /**
     * TODO: 并发互斥 - 在此方法入口增加 per-task 锁，已有同 taskName 执行时直接跳过并记录。
     */
    private void doExecuteAndLog(ScheduledTask task, String taskName, LocalDateTime executeTime,
                                  TriggerSource source, Long triggeredBy) {
        LocalDateTime startedAt = LocalDateTime.now();

        String traceId = TraceIdUtil.getTraceId();
        Long logId = taskLogService.insertRunningLog(taskName, executeTime, source, traceId, triggeredBy, startedAt);

        ScheduledTaskStatus finalStatus = ScheduledTaskStatus.RUNNING;
        String errorMessage = null;
        try {
            log.info("定时任务开始执行: {} | executeTime={} | source={}",
                    taskName, executeTime, source);

            task.execute(executeTime);

            LocalDateTime startedAt = LocalDateTime.now();

            String traceId = TraceIdUtil.getTraceId();
            Long logId = taskLogService.insertRunningLog(taskName, executeTime, source, traceId, triggeredBy, startedAt);

            ScheduledTaskStatus finalStatus = ScheduledTaskStatus.RUNNING;
            String errorMessage = null;
            try {
                log.info("定时任务开始执行: {} | executeTime={} | source={}",
                        taskName, executeTime, source);

                task.execute(executeTime);

                finalStatus = ScheduledTaskStatus.SUCCESS;
                log.info("定时任务执行成功: {}", taskName);
            } catch (Exception e) {
                finalStatus = ScheduledTaskStatus.FAILED;
                errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
                log.error("定时任务执行失败: {}", taskName, e);
            } finally {
                LocalDateTime endedAt = LocalDateTime.now();
                long durationMs = java.time.Duration.between(startedAt, endedAt).toMillis();
                taskLogService.updateLog(logId, finalStatus, errorMessage, endedAt, durationMs);
            }
        }
    }

    /**
     * 安全获取当前用户ID，API手动触发场景下可能无登录态（开发调试），返回 null。
     */
    private Long getCurrentUserIdSafely() {
        try {
            return sessionService.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}