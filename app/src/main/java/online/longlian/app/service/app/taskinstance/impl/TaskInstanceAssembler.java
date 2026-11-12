package online.longlian.app.service.app.taskinstance.impl;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;

import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.TaskFormValuesResultBO;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.bo.app.project.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.common.TaskFormFieldBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceDetailResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskAttachmentResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskSubmissionVersionResultBO;

import online.longlian.app.service.taskform.TaskFormService;
import online.longlian.app.pojo.bo.app.taskinstance.ItemTaskInstanceResultBO;
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

    public ProjectAttachmentDownloadResultBO downloadAttachment(Long taskId, Long orgId, Long fileId) {
        return taskAttachmentPresenter.download(taskId, orgId, fileId);
    }

    public List<ItemTaskInstanceResultBO> assembleInstances(List<TaskInstance> instances, Map<Long, ItemTaskNode> nodeMap) {
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
                    ItemTaskInstanceResultBO.ItemTaskInstanceResultBOBuilder builder = ItemTaskInstanceResultBO.builder()
                            .id(instance.getId())
                            .status(instance.getStatus())
                            .revision(instance.getRevision().toString())
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

    public TaskInstanceDetailResultBO assembleDetail(
            TaskInstance instance, ItemTaskNode node, TaskSubmission submission, Long orgId) {
        TaskInstanceDetailResultBO result = new TaskInstanceDetailResultBO();
        TaskInstanceDetailResultBO.TaskAssigneeResultBO assignee = null;
        if (instance.getAssigneeId() != null) {
            User user = userMapper.selectById(instance.getAssigneeId());
            if (user != null) {
                String avatarUrl = user.getAvatarFileId() != null && user.getAvatarFileId() > 0
                        ? resourceService.getResourceReadUrls(List.of(user.getAvatarFileId()))
                            .values().stream().map(value -> value.getUrl()).findFirst().orElse(null)
                        : null;
                assignee = new TaskInstanceDetailResultBO.TaskAssigneeResultBO(
                        user.getId().toString(), user.getNickname(), avatarUrl);
            }
        }
        result.setTask(new TaskInstanceDetailResultBO.TaskSummaryResultBO(
                instance.getId().toString(), node.getName(), node.getSort(), instance.getStatus().name(), assignee));
        if (submission == null) {
            result.setSubmission(new TaskInstanceDetailResultBO.TaskSubmissionResultBO(
                    "not_submitted", null, List.of()));
            return result;
        }

        List<TaskFormFieldBO> fields = taskFormService.parseFields(node.getMetaSchema());
        TaskFormValuesResultBO validated = taskFormService.parseValues(fields, submission.getMetadata());
        Map<Long, TaskAttachmentResultBO> attachments = taskAttachmentPresenter.present(
                instance.getId(), orgId, validated.resourceIds());
        List<TaskInstanceDetailResultBO.TaskDisplayFieldResultBO> detailFields = displayFields(fields, validated, attachments);
        result.setSubmission(new TaskInstanceDetailResultBO.TaskSubmissionResultBO(
                "submitted", submission.getCreatedAt(), detailFields));
        return result;
    }

    /** 按版本记录的字段结构组装任务实例上的暂存。附件挂在任务实例上。 */
    public TaskInstanceDetailResultBO.TaskDraftResultBO assembleDraft(ItemTaskNode node, TaskInstance instance, Long orgId) {
        if (instance.getDraftMetadata() == null || instance.getDraftMetadata().isBlank()) {
            return null;
        }
        List<TaskFormFieldBO> fields = taskFormService.parseFields(node.getMetaSchema());
        try {
            TaskFormValuesResultBO validated =
                    taskFormService.parseDraftValues(fields, instance.getDraftMetadata());
            Map<Long, TaskAttachmentResultBO> attachments = taskAttachmentPresenter.present(
                    instance.getId(), orgId, validated.resourceIds());
            return new TaskInstanceDetailResultBO.TaskDraftResultBO(
                    instance.getDraftUpdatedAt(), displayFields(fields, validated, attachments), false);
        } catch (AppException exception) {
            return new TaskInstanceDetailResultBO.TaskDraftResultBO(instance.getDraftUpdatedAt(), List.of(), true);
        }
    }

    /** 按表单快照顺序产出展示字段；file 字段挂接已授权附件。 */
    public List<TaskInstanceDetailResultBO.TaskDisplayFieldResultBO> displayFields(
            List<TaskFormFieldBO> fields,
            TaskFormValuesResultBO validated,
            Map<Long, TaskAttachmentResultBO> attachments) {
        List<TaskInstanceDetailResultBO.TaskDisplayFieldResultBO> list = new ArrayList<>(fields.size());
        for (TaskFormFieldBO field : fields) {
            Object value = validated.values().get(field.key());
            if (field.type().equals("file")) {
                TaskAttachmentResultBO file = null;
                if (value != null) {
                    String fileId = (String) ((Map<?, ?>) value).get("fileId");
                    file = attachments.get(taskFormService.parseFileId(fileId));
                }
                list.add(new TaskInstanceDetailResultBO.TaskFileFieldResultBO(field.key(), field.label(), "file", file));
            } else {
                list.add(new TaskInstanceDetailResultBO.TaskTextFieldResultBO(
                        field.key(), field.label(), field.type().equals("textarea") ? "multiline" : "text",
                        value == null ? "未填写" : (String) value));
            }
        }
        return list;
    }

    /** 依次组装每个版本；单个版本存量内容损坏时隔离标记，不影响其它版本展示。 */
    public List<TaskSubmissionVersionResultBO> assembleSubmissionVersions(
            TaskInstance instance, ItemTaskNode node,
            List<TaskSubmission> submissions, Long orgId) {
        if (submissions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> userMap = loadParticipants(submissions);
        Map<Long, String> avatarUrlMap = avatarUrls(userMap.values().stream()
                .map(User::getAvatarFileId).filter(id -> id != null && id > 0).distinct().toList());
        List<TaskFormFieldBO> fields = taskFormService.parseFields(node.getMetaSchema());
        List<TaskSubmissionVersionResultBO> versions = new ArrayList<>(submissions.size());
        for (TaskSubmission submission : submissions) {
            versions.add(assembleVersion(instance, orgId, fields, submission, userMap, avatarUrlMap));
        }
        return versions;
    }

    private TaskSubmissionVersionResultBO assembleVersion(
            TaskInstance instance, Long orgId, List<TaskFormFieldBO> fields,
            TaskSubmission submission, Map<Long, User> userMap, Map<Long, String> avatarUrlMap) {
        TaskSubmissionVersionResultBO version = new TaskSubmissionVersionResultBO();
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
            TaskFormValuesResultBO validated =
                    taskFormService.parseValues(fields, submission.getMetadata());
            Map<Long, TaskAttachmentResultBO> attachments = taskAttachmentPresenter.present(
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

    private TaskSubmissionVersionResultBO.TaskParticipantResultBO participant(
            Long userId, Map<Long, User> userMap, Map<Long, String> avatarUrlMap) {
        if (userId == null) {
            return null;
        }
        User user = userMap.get(userId);
        String avatarUrl = user != null && user.getAvatarFileId() != null && user.getAvatarFileId() > 0
                ? avatarUrlMap.get(user.getAvatarFileId())
                : null;
        return new TaskSubmissionVersionResultBO.TaskParticipantResultBO(userId.toString(),
                user != null ? user.getNickname() : null, avatarUrl);
    }
}
