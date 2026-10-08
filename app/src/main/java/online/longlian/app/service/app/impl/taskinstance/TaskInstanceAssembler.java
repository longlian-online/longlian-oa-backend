package online.longlian.app.service.app.impl.taskinstance;

import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.bo.common.TaskFormField;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.pojo.vo.app.TaskAttachmentVO;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.ArrayList;
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
    private final TaskFormService taskFormService;

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
                .map(User::getAvatarFileId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> avatarUrlMap = avatarFileIds.isEmpty()
                ? Collections.emptyMap()
                : resourceService.getResourceReadUrls(avatarFileIds).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()));

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

    public TaskInstanceDetailVO assembleDetail(
            TaskInstance instance, ItemTaskNode node, TaskSubmission submission, Long orgId) {
        TaskInstanceDetailVO result = new TaskInstanceDetailVO();
        TaskInstanceDetailVO.Assignee assignee = null;
        if (instance.getAssigneeId() != null) {
            User user = userMapper.selectById(instance.getAssigneeId());
            if (user != null) {
                String avatarUrl = user.getAvatarFileId() != null && user.getAvatarFileId() > 0
                        ? resourceService.getResourceReadUrls(List.of(user.getAvatarFileId()))
                            .values().stream().map(value -> value.getUrl()).findFirst().orElse(null)
                        : null;
                assignee = new TaskInstanceDetailVO.Assignee(
                        user.getId().toString(), user.getNickname(), avatarUrl);
            }
        }
        result.setTask(new TaskInstanceDetailVO.Task(
                instance.getId().toString(), node.getName(), node.getSort(), instance.getStatus().name(), assignee));
        if (submission == null) {
            result.setSubmission(new TaskInstanceDetailVO.Submission(
                    "not_submitted", null, List.of()));
            return result;
        }

        List<TaskFormField> fields = taskFormService.parseFields(node.getMetaSchema());
        TaskFormService.ValidatedValues validated = taskFormService.parseValues(fields, submission.getMetadata());
        Map<Long, TaskAttachmentVO> attachments = resourceService.getTaskAttachments(
                instance.getId(), orgId, validated.resourceIds());
        List<TaskInstanceDetailVO.Field> detailFields = new ArrayList<>(fields.size());
        for (TaskFormField field : fields) {
            Object value = validated.values().get(field.key());
            if (field.type().equals("file")) {
                List<TaskAttachmentVO> files;
                if (value == null) {
                    files = List.of();
                } else {
                    String fileId = (String) ((Map<?, ?>) value).get("fileId");
                    files = List.of(attachments.get(taskFormService.parseFileId(fileId)));
                }
                detailFields.add(new TaskInstanceDetailVO.FilesField(
                        field.key(), field.label(), "files", files, files.isEmpty() ? "未填写" : null));
            } else {
                detailFields.add(new TaskInstanceDetailVO.TextField(
                        field.key(), field.label(), field.type().equals("textarea") ? "multiline" : "text",
                        value == null ? "未填写" : (String) value));
            }
        }
        result.setSubmission(new TaskInstanceDetailVO.Submission(
                "submitted", submission.getCreatedAt(), detailFields));
        return result;
    }
}
