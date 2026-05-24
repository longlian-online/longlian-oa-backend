package online.longlian.app.service.app.impl.taskinstance;

import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskInstanceAssembler {

    private final UserMapper userMapper;
    private final ResourceService resourceService;

    public List<ItemTaskInstanceVO> assembleInstances(List<TaskInstance> instances, Map<Long, ItemTaskNode> nodeMap) {
        if (instances.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> assigneeIds = instances.stream()
                .map(TaskInstance::getAssigneeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> userMap = assigneeIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(assigneeIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> avatarFileIds = userMap.values().stream()
                .map(User::getAvatarFileId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> avatarUrlMap = avatarFileIds.stream()
                .collect(Collectors.toMap(Function.identity(), resourceService::getResourceReadUrl));

        return instances.stream()
                .map(instance -> {
                    ItemTaskInstanceVO.ItemTaskInstanceVOBuilder builder = ItemTaskInstanceVO.builder()
                            .id(instance.getId())
                            .status(instance.getStatus())
                            .createdAt(instance.getCreatedAt())
                            .completedAt(instance.getCompletedAt());

                    ItemTaskNode node = nodeMap.get(instance.getItemTaskNodeId());
                    if (node != null) {
                        builder.name(node.getName())
                                .sort(node.getSort())
                                .parallelSort(node.getParallelSort());
                    }

                    if (instance.getAssigneeId() != null) {
                        builder.assigneeId(instance.getAssigneeId());
                        User assignee = userMap.get(instance.getAssigneeId());
                        if (assignee != null) {
                            builder.assigneeNickname(assignee.getNickname());
                            if (assignee.getAvatarFileId() != null) {
                                builder.assigneeAvatarUrl(avatarUrlMap.get(assignee.getAvatarFileId()));
                            }
                        }
                    }
                    return builder.build();
                })
                .toList();
    }
}
