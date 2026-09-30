package online.longlian.app.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.security.CurrentUserContext;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.service.DistributedLockService;
import online.longlian.app.common.util.TraceIdUtil;
import online.longlian.app.pojo.bo.common.ScheduledTaskDefinition;
import online.longlian.app.service.scheduled.ScheduledTaskLogService;
import online.longlian.common.enumeration.ScheduledTaskStatus;
import online.longlian.common.enumeration.TriggerSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskEngine implements SmartLifecycle {

    private final TaskScheduler taskScheduler;
    private final ApplicationContext applicationContext;
    private final CurrentUserContext currentUserContext;
    private final ScheduledTaskLogService taskLogService;
    private final DistributedLockService lockService;
    private final Clock clock;

    /**
     * 所有已注册的任务，按 taskName 索引
     */
    private final Map<String, ScheduledTask> taskMap = new ConcurrentHashMap<>();

    /**
     * 注册时校验通过的任务定义，避免运行期间重复构造或读取发生变化的定义。
     */
    private final Map<String, ScheduledTaskDefinition> definitionMap = new ConcurrentHashMap<>();

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
        Map<String, ScheduledTask> beans = applicationContext.getBeansOfType(ScheduledTask.class);
        Map<String, TaskRegistration> registrations = collectRegistrations(beans);

        for (TaskRegistration registration : registrations.values()) {
            ScheduledTask task = registration.task();
            ScheduledTaskDefinition def = registration.definition();
            taskMap.put(def.getTaskName(), task);
            definitionMap.put(def.getTaskName(), def);
            if (def.isEnabled()) {
                scheduleCron(task, def);
                log.info("已注册定时任务：{} | 自动调度=已启用 | Cron表达式={}",
                        def.getLabel(), def.getCronExpression());
            } else {
                log.info("已注册定时任务：{} | 自动调度=未启用，可手动触发", def.getLabel());
            }
        }
        running.set(true);
        log.info("定时任务注册完成，共注册 {} 个任务，其中 {} 个已启用自动调度",
                registrations.size(), registrations.values().stream()
                        .map(TaskRegistration::definition)
                        .filter(ScheduledTaskDefinition::isEnabled)
                        .count());
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
        long deadline = clock.millis() + shutdownTimeout.toMillis();

        for (Map.Entry<String, Thread> entry : snapshot.entrySet()) {
            String taskName = entry.getKey();
            Thread thread = entry.getValue();
            long remaining = deadline - clock.millis();
            if (remaining > 0) {
                try {
                    thread.join(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("等待定时任务完成时被中断：{}", registeredTaskLabel(taskName));
                    break;
                }
            }
            if (thread.isAlive()) {
                log.warn("定时任务未能在停机超时时间内完成：{}", registeredTaskLabel(taskName));
            } else {
                log.info("定时任务已在系统关闭前执行完成：{}", registeredTaskLabel(taskName));
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
            log.warn("系统正在关闭，拒绝手动触发定时任务：{}", registeredTaskLabel(taskName));
            return;
        }
        ScheduledTask task = getTask(taskName);
        ScheduledTaskDefinition definition = definitionMap.get(taskName);
        LocalDateTime execTime = executeTime != null ? executeTime : LocalDateTime.now(clock);
        Long userId = getCurrentUserIdSafely();

        executeAndLog(task, definition, execTime, TriggerSource.MANUAL, userId);
    }

    // ==================== 内部实现 ====================

    private void scheduleCron(ScheduledTask task, ScheduledTaskDefinition definition) {
        String cronExpression = definition.getCronExpression();
        taskScheduler.schedule(
                () -> {
                    if (shutdown) {
                        log.info("系统正在关闭，跳过定时任务：{}", definition.getLabel());
                        return;
                    }
                    executeAndLog(task, definition, LocalDateTime.now(clock), TriggerSource.SCHEDULED, null);
                },
                new CronTrigger(cronExpression));
    }

    /**
     * 执行任务并记录日志，同时将执行线程注册到 runningTasks 以支持停机等待。
     * 调用定时任务的统一入口，负责获取分布式锁、记录日志、捕获异常等公共逻辑，确保无论是 cron 触发还是手动触发都能正确记录和管理执行状态。
     */
    private void executeAndLog(ScheduledTask task, ScheduledTaskDefinition definition, LocalDateTime executeTime,
            TriggerSource source, Long triggeredBy) {
        String taskName = definition.getTaskName();
        String lockKey = "scheduled-task:" + taskName;
        try (var lock = lockService.tryAcquire(lockKey, 0, TimeUnit.SECONDS)) {
            if (lock == null) {
                log.warn("跳过定时任务：{} | 原因=其他实例可能正在执行，未获得分布式锁 | 触发方式={} | 业务执行时间={}",
                        definition.getLabel(), source.getDesc(), executeTime);
                return;
            }
            Thread currentThread = Thread.currentThread();
            runningTasks.put(taskName, currentThread);
            try {
                doExecuteAndLog(task, definition, executeTime, source, triggeredBy);
            } finally {
                runningTasks.remove(taskName);
            }
        }
    }

    /**
     * 执行任务并记录日志。
     * 不应该方法直接调用，而应通过 executeAndLog 获取分布式锁和管理执行线程，确保日志记录和停机等待的正确性。
     */
    private void doExecuteAndLog(ScheduledTask task, ScheduledTaskDefinition definition, LocalDateTime executeTime,
            TriggerSource source, Long triggeredBy) {
        String taskName = definition.getTaskName();
        LocalDateTime startedAt = LocalDateTime.now(clock);

        String traceId = TraceIdUtil.getTraceId();
        Long logId = taskLogService.insertRunningLog(taskName, executeTime, source, traceId, triggeredBy, startedAt);

        ScheduledTaskStatus finalStatus = ScheduledTaskStatus.RUNNING;
        String errorMessage = null;
        Exception executionException = null;
        try {
            log.info("开始执行定时任务：{} | 触发方式={} | 业务执行时间={} | 触发人ID={}",
                    definition.getLabel(), source.getDesc(), executeTime, triggeredBy);

            task.execute(executeTime);

            finalStatus = ScheduledTaskStatus.SUCCESS;
        } catch (Exception e) {
            finalStatus = ScheduledTaskStatus.FAILED;
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            executionException = e;
        } finally {
            LocalDateTime endedAt = LocalDateTime.now(clock);
            long durationMs = java.time.Duration.between(startedAt, endedAt).toMillis();
            if (finalStatus == ScheduledTaskStatus.SUCCESS) {
                log.info("定时任务执行成功：{} | 触发方式={} | 耗时={}毫秒",
                        definition.getLabel(), source.getDesc(), durationMs);
            } else {
                log.error("定时任务执行失败：{} | 触发方式={} | 耗时={}毫秒",
                        definition.getLabel(), source.getDesc(), durationMs, executionException);
            }
            taskLogService.updateLog(logId, finalStatus, errorMessage, endedAt, durationMs);
        }
    }

    /**
     * 在创建任何调度前收集任务并检查名称冲突，避免重复名称导致部分任务已经注册。
     */
    private Map<String, TaskRegistration> collectRegistrations(Map<String, ScheduledTask> beans) {
        Map<String, TaskRegistration> registrations = new LinkedHashMap<>();
        for (Map.Entry<String, ScheduledTask> entry : beans.entrySet()) {
            ScheduledTaskDefinition definition = entry.getValue().getDefinition();
            if (definition == null) {
                throw new IllegalStateException("定时任务注册失败：任务定义不能为空，Bean名称=" + entry.getKey());
            }
            TaskRegistration registration = new TaskRegistration(entry.getValue(), definition);
            if (registrations.putIfAbsent(definition.getTaskName(), registration) != null) {
                throw new IllegalStateException("定时任务注册失败：任务标识重复，任务标识="
                        + definition.getTaskName());
            }
        }
        return registrations;
    }

    private String registeredTaskLabel(String taskName) {
        ScheduledTaskDefinition definition = definitionMap.get(taskName);
        return definition == null ? taskName : definition.getLabel();
    }

    private record TaskRegistration(ScheduledTask task, ScheduledTaskDefinition definition) {
    }

    /**
     * 安全获取当前用户ID，API手动触发场景下可能无登录态（开发调试），返回 null。
     */
    private Long getCurrentUserIdSafely() {
        try {
            return currentUserContext.requireUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
