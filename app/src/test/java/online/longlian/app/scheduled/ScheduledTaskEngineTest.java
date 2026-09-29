package online.longlian.app.scheduled;

import online.longlian.app.pojo.bo.common.ScheduledTaskDefinition;
import online.longlian.app.common.security.CurrentUserContext;
import online.longlian.app.service.scheduled.ScheduledTaskLogService;
import online.longlian.common.enumeration.TriggerSource;
import online.longlian.common.service.DistributedLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ScheduledTaskEngineTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-29T02:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private CurrentUserContext currentUserContext;
    @Mock
    private ScheduledTaskLogService taskLogService;
    @Mock
    private DistributedLockService lockService;

    @Test
    void shouldUseWatchdogLockForScheduledTask() {
        ScheduledTask task = mock(ScheduledTask.class);
        ScheduledTaskDefinition definition = ScheduledTaskDefinition.builder()
                .taskName("long-running")
                .description("执行长时间运行任务")
                .enabled(false)
                .build();
        DistributedLockService.Lock lock = mock(DistributedLockService.Lock.class);
        when(task.getDefinition()).thenReturn(definition);
        when(applicationContext.getBeansOfType(ScheduledTask.class)).thenReturn(Map.of("longRunningTask", task));
        when(currentUserContext.requireUserId()).thenReturn(1L);
        when(lockService.tryAcquire("scheduled-task:long-running", 0, TimeUnit.SECONDS)).thenReturn(lock);
        when(taskLogService.insertRunningLog(eq("long-running"), any(), eq(TriggerSource.MANUAL), any(), eq(1L), any()))
                .thenReturn(1L);
        ScheduledTaskEngine engine = new ScheduledTaskEngine(
                taskScheduler, applicationContext, currentUserContext, taskLogService, lockService, CLOCK);
        engine.start();
        engine.trigger("long-running", LocalDateTime.now(CLOCK));

        verify(lockService).tryAcquire("scheduled-task:long-running", 0, TimeUnit.SECONDS);
    }

    @Test
    void shouldRejectTaskWithoutChineseDescription() {
        ScheduledTask task = mock(ScheduledTask.class);
        ScheduledTaskDefinition definition = ScheduledTaskDefinition.builder()
                .taskName("missing-description")
                .description(" ")
                .enabled(false)
                .build();
        when(task.getDefinition()).thenReturn(definition);
        when(applicationContext.getBeansOfType(ScheduledTask.class)).thenReturn(Map.of("missingDescriptionTask", task));
        ScheduledTaskEngine engine = new ScheduledTaskEngine(
                taskScheduler, applicationContext, currentUserContext, taskLogService, lockService, CLOCK);

        assertThatThrownBy(engine::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必须填写中文任务描述")
                .hasMessageContaining("missing-description");
        assertThat(engine.isRunning()).isFalse();
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void shouldRejectDuplicateTaskNameBeforeScheduling() {
        ScheduledTask firstTask = mock(ScheduledTask.class);
        ScheduledTask secondTask = mock(ScheduledTask.class);
        ScheduledTaskDefinition firstDefinition = ScheduledTaskDefinition.builder()
                .taskName("duplicate-task")
                .description("第一个重复任务")
                .cronExpression("0 0/5 * * * ?")
                .enabled(true)
                .build();
        ScheduledTaskDefinition secondDefinition = ScheduledTaskDefinition.builder()
                .taskName("duplicate-task")
                .description("第二个重复任务")
                .enabled(false)
                .build();
        when(firstTask.getDefinition()).thenReturn(firstDefinition);
        when(secondTask.getDefinition()).thenReturn(secondDefinition);
        when(applicationContext.getBeansOfType(ScheduledTask.class)).thenReturn(Map.of(
                "firstTask", firstTask,
                "secondTask", secondTask));
        ScheduledTaskEngine engine = new ScheduledTaskEngine(
                taskScheduler, applicationContext, currentUserContext, taskLogService, lockService, CLOCK);

        assertThatThrownBy(engine::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("任务标识重复")
                .hasMessageContaining("duplicate-task");
        assertThat(engine.isRunning()).isFalse();
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void shouldWriteDetailedChineseExecutionLog(CapturedOutput output) {
        ScheduledTask task = mock(ScheduledTask.class);
        ScheduledTaskDefinition definition = ScheduledTaskDefinition.builder()
                .taskName("resource-cleanup")
                .description("清理已废弃资源的实际存储文件")
                .enabled(false)
                .build();
        DistributedLockService.Lock lock = mock(DistributedLockService.Lock.class);
        LocalDateTime executeTime = LocalDateTime.now(CLOCK);
        when(task.getDefinition()).thenReturn(definition);
        when(applicationContext.getBeansOfType(ScheduledTask.class)).thenReturn(Map.of("resourceCleanupTask", task));
        when(currentUserContext.requireUserId()).thenReturn(1L);
        when(lockService.tryAcquire("scheduled-task:resource-cleanup", 0, TimeUnit.SECONDS)).thenReturn(lock);
        when(taskLogService.insertRunningLog(eq("resource-cleanup"), eq(executeTime),
                eq(TriggerSource.MANUAL), any(), eq(1L), any())).thenReturn(1L);
        ScheduledTaskEngine engine = new ScheduledTaskEngine(
                taskScheduler, applicationContext, currentUserContext, taskLogService, lockService, CLOCK);

        engine.start();
        engine.trigger("resource-cleanup", executeTime);

        assertThat(output)
                .contains("清理已废弃资源的实际存储文件（resource-cleanup）")
                .contains("触发方式=手动触发")
                .contains("业务执行时间=2026-09-29T02:00")
                .contains("定时任务执行成功")
                .contains("耗时=0毫秒");
    }
}
