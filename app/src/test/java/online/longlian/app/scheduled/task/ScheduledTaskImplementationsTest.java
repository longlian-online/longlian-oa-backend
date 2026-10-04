package online.longlian.app.scheduled.task;

import online.longlian.app.service.resource.DeprecatedResourceCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
class ScheduledTaskImplementationsTest {

    @Test
    void shouldDescribeAndLogHeartbeat(CapturedOutput output) {
        HeartbeatTask task = new HeartbeatTask();
        LocalDateTime executeTime = LocalDateTime.of(2026, 9, 30, 12, 0);

        assertThat(task.getDefinition().getTaskName()).isEqualTo("heartbeat");
        assertThat(task.getDefinition().getDescription()).contains("心跳检测");
        assertThat(task.getDefinition()).isSameAs(task.getDefinition());
        task.execute(executeTime);

        assertThat(output).contains("已完成定时任务调度链路心跳检测")
                .contains("业务执行时间=2026-09-30T12:00");
    }

    @Test
    void shouldDelegateResourceCleanupWithProvidedTime() {
        DeprecatedResourceCleanupService cleanupService = mock(DeprecatedResourceCleanupService.class);
        DeprecatedResourceCleanupTask task = new DeprecatedResourceCleanupTask(cleanupService);
        LocalDateTime executeTime = LocalDateTime.of(2026, 9, 30, 12, 0);

        assertThat(task.getDefinition().getTaskName()).isEqualTo("resource-cleanup");
        assertThat(task.getDefinition()).isSameAs(task.getDefinition());
        task.execute(executeTime);

        verify(cleanupService).cleanup(executeTime);
    }
}
