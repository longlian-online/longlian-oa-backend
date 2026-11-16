package online.longlian.app.scheduled.task;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.common.ScheduledTaskDefinitionBO;
import online.longlian.app.scheduled.ScheduledTask;
import online.longlian.app.service.resource.ResourceDeprecationService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ResourceDeprecationTask implements ScheduledTask {
    private static final ScheduledTaskDefinitionBO DEFINITION = new ScheduledTaskDefinitionBO(
            "resource-deprecation",
            "把业务数据不再引用的已激活资源标为弃置，供资源清理任务在保留期后删除",
            "0 2/5 * * * ?",
            true);

    private final ResourceDeprecationService deprecationService;

    @Override
    public ScheduledTaskDefinitionBO getDefinition() {
        return DEFINITION;
    }

    @Override
    public void execute(LocalDateTime executeTime) {
        deprecationService.deprecateUnreferenced(executeTime);
    }
}
