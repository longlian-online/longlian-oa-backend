package online.longlian.app.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.service.DistributedLockService;
import online.longlian.app.common.util.TraceIdUtil;
import online.longlian.app.pojo.bo.ScheduledTaskDefinition;
import online.longlian.app.service.scheduled.ScheduledTaskLogService;
import online.longlian.app.service.user.SessionService;
import online.longlian.common.enumeration.ScheduledTaskStatus;
import online.longlian.common.enumeration.TriggerSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务调度引擎。
 * <p>
 * 启动时扫描所有 {@link ScheduledTask} Bean，根据定义中的 cron 表达式自动注册调度；
 * 同时提供手动触发接口供 API 调用。
 * <p>
 * 内部通过 traceId（{@link TraceIdUtil}）、triggerSource、triggeredBy 记录元信息用于日志和排障，
 * 但这些信息不暴露给业务任务——业务任务只接收一个 {@code LocalDateTime executeTime} 参数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskEngine implements ApplicationRunner {

    private final TaskScheduler taskScheduler;
    private final ApplicationContext applicationContext;
    private final SessionService sessionService;
    private final ScheduledTaskLogService taskLogService;
    private final DistributedLockService lockService;

    /**
     * 所有已注册的任务，按 taskName 索引
     */
    private final Map<String, ScheduledTask> taskMap = new ConcurrentHashMap<>();

    // ==================== 生命周期 ====================

    @Override
    public void run(ApplicationArguments args) {
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
        ScheduledTask task = getTask(taskName);
        LocalDateTime execTime = executeTime != null ? executeTime : LocalDateTime.now();
        Long userId = getCurrentUserIdSafely();

        executeAndLog(task, taskName, execTime, TriggerSource.MANUAL, userId);
    }

    // ==================== 内部实现 ====================

    private void scheduleCron(String taskName, @NonNull String cronExpression) {
        ScheduledTask task = taskMap.get(taskName);
        taskScheduler.schedule(
                () -> executeAndLog(task, taskName, LocalDateTime.now(), TriggerSource.SCHEDULED, null),
                new CronTrigger(cronExpression)
        );
    }


    /**
     * 执行任务并记录日志。
     */
    private void executeAndLog(ScheduledTask task, String taskName, LocalDateTime executeTime,
                               TriggerSource source, Long triggeredBy) {
        String lockKey = "scheduled-task:" + taskName;
        try (var lock = lockService.tryAcquire(lockKey, 0, 30, TimeUnit.SECONDS)) {
            if (lock == null) {
                log.warn("定时任务跳过（锁未获取）: {} | executeTime={} | source={}", taskName, executeTime, source);
                return;
            }

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