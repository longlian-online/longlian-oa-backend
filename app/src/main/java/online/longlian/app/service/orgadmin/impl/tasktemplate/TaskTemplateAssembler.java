package online.longlian.app.service.orgadmin.impl.tasktemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateDetailResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateNodeResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskTemplateAssembler {

    private final UserMapper userMapper;
    private final BaseTaskMapper baseTaskMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final ResourceService resourceService;

    public List<TaskTemplateListResultBO> assembleList(List<TaskTemplate> templates) {
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = resolveCreators(templates);

        return templates.stream()
                .map(template -> TaskTemplateListResultBO.builder()
                        .id(template.getId())
                        .name(template.getName())
                        .description(template.getDescription())
                        .status(template.getStatus())
                        .refCount(template.getRefCount())
                        .creatorId(template.getCreatorId())
                        .creatorNickname(userMap.getOrDefault(template.getCreatorId(), User.builder().build()).getNickname())
                        .createdAt(template.getCreatedAt())
                        .build())
                .toList();
    }

    public TaskTemplateDetailResultBO assembleDetail(TaskTemplate template) {
        List<TaskTemplateNode> nodes = taskTemplateNodeMapper.selectList(
                new LambdaQueryWrapper<TaskTemplateNode>()
                        .eq(TaskTemplateNode::getTaskTemplateId, template.getId())
                        .isNull(TaskTemplateNode::getDeletedAt)
                        .orderByAsc(TaskTemplateNode::getSort)
        );

        Map<Long, User> userMap = resolveCreators(List.of(template));

        List<Long> baseTaskIds = nodes.stream().map(TaskTemplateNode::getBaseTaskId).distinct().toList();
        Map<Long, BaseTask> baseTaskMap = baseTaskIds.isEmpty()
                ? Collections.emptyMap()
                : baseTaskMapper.selectBatchIds(baseTaskIds).stream()
                        .collect(Collectors.toMap(BaseTask::getId, Function.identity()));

        List<Long> iconFileIds = baseTaskMap.values().stream()
                .map(BaseTask::getIconFileId)
                .filter(fileId -> fileId != null && fileId > 0)
                .distinct()
                .toList();
        Map<Long, String> iconUrlMap = iconFileIds.isEmpty()
                ? Collections.emptyMap()
                : resourceService.getResourceReadUrls(iconFileIds).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getUrl()));

        List<TaskTemplateNodeResultBO> nodeResults = nodes.stream()
                .map(node -> {
                    BaseTask baseTask = baseTaskMap.get(node.getBaseTaskId());
                    return TaskTemplateNodeResultBO.builder()
                            .id(node.getId())
                            .baseTaskId(node.getBaseTaskId())
                            .baseTaskName(baseTask != null ? baseTask.getName() : null)
                            .baseTaskIconUrl(baseTask != null && baseTask.getIconFileId() != null
                                    ? iconUrlMap.get(baseTask.getIconFileId())
                                    : null)
                            .metaSchema(baseTask != null ? baseTask.getMetaSchema() : null)
                            .sort(node.getSort())
                            .parallelSort(node.getParallelSort())
                            .build();
                })
                .toList();

        return TaskTemplateDetailResultBO.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .status(template.getStatus())
                .refCount(template.getRefCount())
                .creatorId(template.getCreatorId())
                .creatorNickname(userMap.getOrDefault(template.getCreatorId(), User.builder().build()).getNickname())
                .createdAt(template.getCreatedAt())
                .nodes(nodeResults)
                .build();
    }

    private Map<Long, User> resolveCreators(List<TaskTemplate> templates) {
        List<Long> creatorIds = templates.stream().map(TaskTemplate::getCreatorId).distinct().toList();
        return userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
