package online.longlian.app.scheduled;

import online.longlian.app.pojo.bo.common.ScheduledTaskDefinition;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.scheduled.ScheduledTaskLogService;
import online.longlian.common.enumeration.TriggerSource;
import online.longlian.common.service.DistributedLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskEngineTest {

    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private SessionService sessionService;
    @Mock
    private ScheduledTaskLogService taskLogService;
    @Mock
    private DistributedLockService lockService;

    @Test
    void shouldUseWatchdogLockForScheduledTask() {
        ScheduledTask task = mock(ScheduledTask.class);
        ScheduledTaskDefinition definition = ScheduledTaskDefinition.builder()
                .taskName("long-running")
                .enabled(false)
                .build();
        DistributedLockService.Lock lock = mock(DistributedLockService.Lock.class);
        when(task.getDefinition()).thenReturn(definition);
        when(applicationContext.getBeansOfType(ScheduledTask.class)).thenReturn(Map.of("longRunningTask", task));
        when(sessionService.getCurrentUserId()).thenReturn(1L);
        when(lockService.tryAcquire("scheduled-task:long-running", 0, TimeUnit.SECONDS)).thenReturn(lock);
        when(taskLogService.insertRunningLog(eq("long-running"), any(), eq(TriggerSource.MANUAL), any(), eq(1L), any()))
                .thenReturn(1L);
        ScheduledTaskEngine engine = new ScheduledTaskEngine(
                taskScheduler, applicationContext, sessionService, taskLogService, lockService);
        engine.start();
        engine.trigger("long-running", LocalDateTime.now());

        verify(lockService).tryAcquire("scheduled-task:long-running", 0, TimeUnit.SECONDS);
    }
}
