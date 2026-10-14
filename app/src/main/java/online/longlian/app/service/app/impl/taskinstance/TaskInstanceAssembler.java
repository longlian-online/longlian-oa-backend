package online.longlian.app.service.app.impl.taskinstance;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;

import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.bo.common.TaskFormField;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.pojo.vo.app.TaskAttachmentVO;
import online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO;

import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskInstanceAssembler {

    private final UserMapper userMapper;
    private final ResourceService resourceService;
    private final TaskFormService taskFormService;
    private final TaskAttachmentPresenter taskAttachmentPresenter;

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
        Map<Long, TaskAttachmentVO> attachments = taskAttachmentPresenter.present(
                instance.getId(), orgId, validated.resourceIds());
        List<TaskInstanceDetailVO.Field> detailFields = displayFields(fields, validated, attachments);
        result.setSubmission(new TaskInstanceDetailVO.Submission(
                "submitted", submission.getCreatedAt(), detailFields));
        return result;
    }

    /** 按表单快照顺序产出展示字段；file 字段挂接已授权附件。 */
    public List<TaskInstanceDetailVO.Field> displayFields(
            List<TaskFormField> fields,
            TaskFormService.ValidatedValues validated,
            Map<Long, TaskAttachmentVO> attachments) {
        List<TaskInstanceDetailVO.Field> list = new ArrayList<>(fields.size());
        for (TaskFormField field : fields) {
            Object value = validated.values().get(field.key());
            if (field.type().equals("file")) {
                TaskAttachmentVO file = null;
                if (value != null) {
                    String fileId = (String) ((Map<?, ?>) value).get("fileId");
                    file = attachments.get(taskFormService.parseFileId(fileId));
                }
                list.add(new TaskInstanceDetailVO.FileField(field.key(), field.label(), "file", file));
            } else {
                list.add(new TaskInstanceDetailVO.TextField(
                        field.key(), field.label(), field.type().equals("textarea") ? "multiline" : "text",
                        value == null ? "未填写" : (String) value));
            }
        }
        return list;
    }

    /** 依次组装每个版本；单个版本存量内容损坏时隔离标记，不影响其它版本展示。 */
    public List<TaskSubmissionVersionVO> assembleSubmissionVersions(
            TaskInstance instance, ItemTaskNode node,
            List<TaskSubmission> submissions, Long orgId) {
        if (submissions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> userMap = loadParticipants(submissions);
        Map<Long, String> avatarUrlMap = avatarUrls(userMap.values().stream()
                .map(User::getAvatarFileId).filter(id -> id != null && id > 0).distinct().toList());
        List<TaskFormField> fields = taskFormService.parseFields(node.getMetaSchema());
        List<TaskSubmissionVersionVO> versions = new ArrayList<>(submissions.size());
        for (TaskSubmission submission : submissions) {
            versions.add(assembleVersion(instance, orgId, fields, submission, userMap, avatarUrlMap));
        }
        return versions;
    }

    private TaskSubmissionVersionVO assembleVersion(
            TaskInstance instance, Long orgId, List<TaskFormField> fields,
            TaskSubmission submission, Map<Long, User> userMap, Map<Long, String> avatarUrlMap) {
        TaskSubmissionVersionVO version = new TaskSubmissionVersionVO();
        version.setVersionNo(submission.getVersionNo());
        version.setStatus(submission.getStatus().name());
        version.setSubmittedAt(submission.getCreatedAt());
        version.setSubmitter(participant(submission.getSubmitterId(), userMap, avatarUrlMap));
        if (submission.getStatus() == TaskSubmissionStatus.REJECTED) {
            version.setReviewedAt(submission.getReviewedAt());
            version.setReviewer(participant(submission.getReviewerId(), userMap, avatarUrlMap));
            version.setReviewComment(submission.getReviewComment());
        }
        try {
            TaskFormService.ValidatedValues validated =
                    taskFormService.parseValues(fields, submission.getMetadata());
            Map<Long, TaskAttachmentVO> attachments = taskAttachmentPresenter.present(
                    instance.getId(), orgId, validated.resourceIds());
            version.setFields(displayFields(fields, validated, attachments));
        } catch (AppException exception) {
            version.setUnreadable(true);
            version.setFields(Collections.emptyList());
        }
        return version;
    }

    /** 批量加载版本中的提交人与打回人。 */
    private Map<Long, User> loadParticipants(List<TaskSubmission> submissions) {
        Set<Long> userIds = new HashSet<>();
        for (TaskSubmission submission : submissions) {
            userIds.add(submission.getSubmitterId());
            if (submission.getReviewerId() != null) {
                userIds.add(submission.getReviewerId());
            }
        }
        return userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, String> avatarUrls(List<Long> avatarFileIds) {
        if (avatarFileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return resourceService.getResourceReadUrls(avatarFileIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()));
    }

    private TaskSubmissionVersionVO.Person participant(
            Long userId, Map<Long, User> userMap, Map<Long, String> avatarUrlMap) {
        if (userId == null) {
            return null;
        }
        User user = userMap.get(userId);
        String avatarUrl = user != null && user.getAvatarFileId() != null && user.getAvatarFileId() > 0
                ? avatarUrlMap.get(user.getAvatarFileId())
                : null;
        return new TaskSubmissionVersionVO.Person(userId.toString(),
                user != null ? user.getNickname() : null, avatarUrl);
    }
}
