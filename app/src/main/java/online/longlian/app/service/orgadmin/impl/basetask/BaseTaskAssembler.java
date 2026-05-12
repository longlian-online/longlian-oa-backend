package online.longlian.app.service.orgadmin.impl.basetask;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BaseTaskAssembler {

    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final ResourceService resourceService;

    public List<BaseTaskListResultBO> assembleBaseTaskList(List<BaseTask> tasks) {
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> iconUrlMap;
        List<Long> iconFileIds = tasks.stream()
                .map(BaseTask::getIconFileId)
                .filter(fileId -> fileId != null && fileId > 0)
                .distinct()
                .toList();
        if (iconFileIds.isEmpty()) {
            iconUrlMap = Collections.emptyMap();
        } else {
            iconUrlMap = resourceService.getResourceReadUrls(iconFileIds).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getUrl()));
        }

        List<Long> baseTaskIds = tasks.stream().map(BaseTask::getId).toList();
        List<TaskTemplateNode> nodes = taskTemplateNodeMapper.selectList(
                new LambdaQueryWrapper<TaskTemplateNode>()
                        .in(TaskTemplateNode::getBaseTaskId, baseTaskIds)
                        .isNull(TaskTemplateNode::getDeletedAt)
        );
        Map<Long, Long> refCountMap = nodes.stream()
                .collect(Collectors.groupingBy(TaskTemplateNode::getBaseTaskId, Collectors.counting()));

        // 组装结果
        return tasks.stream()
                .map(task -> BaseTaskListResultBO.builder()
                        .id(task.getId())
                        .name(task.getName())
                        .description(task.getDescription())
                        .iconUrl(task.getIconFileId() != null ? iconUrlMap.get(task.getIconFileId()) : null)
                        .metaSchema(task.getMetaSchema())
                        .refCount(refCountMap.getOrDefault(task.getId(), 0L).intValue())
                        .status(task.getStatus())
                        .createdAt(task.getCreatedAt())
                        .build())
                .toList();
    }
}