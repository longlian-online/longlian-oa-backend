package online.longlian.app.scheduled.task;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.ScheduledTaskDefinition;
import online.longlian.app.scheduled.ScheduledTask;
import online.longlian.app.service.resource.DeprecatedResourceCleanupService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DeprecatedResourceCleanupTask implements ScheduledTask {
    private final DeprecatedResourceCleanupService deprecatedResourceCleanupService;

    @Override
    public ScheduledTaskDefinition getDefinition() {
        return ScheduledTaskDefinition.builder()
                .taskName("resource-cleanup")
                .description("清理已废弃资源的物理存储文件")
                .cronExpression("0 0/5 * * * ?")
                .enabled(true)
                .build();
    }

    @Override
    public void execute(LocalDateTime executeTime) {
        deprecatedResourceCleanupService.cleanup(executeTime);
    }
}
