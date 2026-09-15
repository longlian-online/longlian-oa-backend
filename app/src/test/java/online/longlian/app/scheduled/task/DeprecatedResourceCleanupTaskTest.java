package online.longlian.app.scheduled.task;

import online.longlian.app.service.resource.DeprecatedResourceCleanupService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DeprecatedResourceCleanupTaskTest {
    @Test
    public void shouldDelegateScheduledExecutionWithProvidedTime() {
        DeprecatedResourceCleanupService cleanupService = mock(DeprecatedResourceCleanupService.class);
        DeprecatedResourceCleanupTask task = new DeprecatedResourceCleanupTask(cleanupService);
        LocalDateTime executeTime = LocalDateTime.of(2026, 9, 16, 12, 0);

        task.execute(executeTime);

        verify(cleanupService).cleanup(executeTime);
    }

    @Test
    public void shouldExposeEnabledFiveMinuteCleanupDefinition() {
        DeprecatedResourceCleanupTask task = new DeprecatedResourceCleanupTask(mock(DeprecatedResourceCleanupService.class));

        assertThat(task.getDefinition().getTaskName()).isEqualTo("resource-cleanup");
        assertThat(task.getDefinition().getCronExpression()).isEqualTo("0 0/5 * * * ?");
        assertThat(task.getDefinition().isEnabled()).isTrue();
    }
}
