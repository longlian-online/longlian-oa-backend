package online.longlian.app.scheduled.task;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.common.ScheduledTaskDefinition;
import online.longlian.app.scheduled.ScheduledTask;
import online.longlian.app.service.resource.UnboundResourceCleanupService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UnboundResourceCleanupTask implements ScheduledTask {
    private static final ScheduledTaskDefinition DEFINITION = new ScheduledTaskDefinition(
            "resource-cleanup", "回收过期的未绑定和已废弃文件，并核对存储大小", "0 0/5 * * * ?", true);

    private final UnboundResourceCleanupService cleanupService;

    @Override
    public ScheduledTaskDefinition getDefinition() {
        return DEFINITION;
    }

    @Override
    public void execute(LocalDateTime executeTime) {
        cleanupService.cleanup(executeTime);
    }
}
