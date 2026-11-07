package online.longlian.app.service.orgadmin.impl.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.ActivatedResourceRead;
import online.longlian.app.pojo.bo.common.TaskFormField;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDiscardParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDownloadParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileEntriesResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileEntryResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileListParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.service.orgadmin.OrgFileService;
import online.longlian.app.service.resource.ResourceDeprecationService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.OrgFileEntryKind;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;

/**
 * 组织文件页是跨企划、项目和任务提交的只读目录。
 * 各自主 Service 没有「当前有效提交里的文件」这一查询；拆成无业务含义的转发只会把同一套筛选再写一遍，
 * 所以这里直接读对应 Mapper，资源签名仍交给资源服务。
 */
@Service
@RequiredArgsConstructor
public class OrgFileServiceImpl implements OrgFileService {

    private static final String TASK_SUBMIT = "task_submit";
    private static final String AVAILABLE = "available";
    private static final String UNAVAILABLE = "unavailable";

    private final ProjectMapper projectMapper;
    private final ItemMapper itemMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final UserMapper userMapper;
    private final TaskFormService taskFormService;
    private final ResourceService resourceService;
    private final ResourceDeprecationService resourceDeprecationService;
    private final Clock clock;

    @Override
    public OrgFileEntriesResultBO list(OrgFileListParamsBO params) {
        if (params.getItemId() != null) {
            return listFiles(params);
        }
        if (params.getProjectId() != null) {
            return listItems(params);
        }
        return listProjects(params);
    }

    @Override
    public ProjectAttachmentDownloadResultBO download(OrgFileDownloadParamsBO params) {
        TaskInstance instance = taskInstanceMapper.selectById(params.getTaskInstanceId());
        if (instance == null) {
            throw missing("附件不存在");
        }
        requireItem(instance.getItemId(), instance.getProjectId(), params.getOrgId());
        if (!currentFileIds(instance).contains(params.getFileId())) {
            throw missing("附件不存在");
        }
        ActivatedResource resource = resourceService
                .findActivated(TASK_SUBMIT, instance.getId(), params.getOrgId(), List.of(params.getFileId()))
                .get(params.getFileId());
        if (resource == null) {
            throw missing("附件不存在");
        }
        ActivatedResourceRead read = resourceService.signDownload(resource);
        return ProjectAttachmentDownloadResultBO.builder()
                .readUrl(read.readUrl())
                .expireAt(read.expiresAt())
                .build();
    }

    @Override
    @Transactional
    public void discard(OrgFileDiscardParamsBO params) {
        if (params.getFieldKey() == null || params.getFieldKey().isBlank()) {
            throw new AppException(ResultCode.PARAM_ERROR, "提交字段不能为空");
        }
        TaskInstance instance = taskInstanceMapper.selectById(params.getTaskInstanceId());
        if (instance == null) {
            throw missing("附件不存在");
        }
        requireItem(instance.getItemId(), instance.getProjectId(), params.getOrgId());
        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        TaskSubmission submission = currentSubmissions(instance.getItemId()).get(instance.getId());
        List<TaskFormField> fields = node == null ? null : readFields(node.getMetaSchema());
        TaskFormService.ValidatedValues values = fields == null || submission == null
                ? null : readValues(fields, submission.getMetadata());
        TaskFormField field = fields == null ? null : fields.stream()
                .filter(item -> params.getFieldKey().equals(item.key()))
                .findFirst()
                .orElse(null);
        if (field == null || values == null || !params.getFileId().equals(fileId(field, values))) {
            throw missing("附件不存在");
        }
        // 不改提交 JSON。必填文件被清空会让任务详情无法读取；弃置后详情和历史版本显示为不可用。
        boolean deprecated = resourceDeprecationService.deprecateTaskSubmitFile(
                params.getFileId(), instance.getId(), params.getOrgId(), LocalDateTime.now(clock));
        if (!deprecated) {
            throw new AppException(ResultCode.OPERATION_FAIL, "文件仍被其他资料使用，不能删除");
        }
    }

    private OrgFileEntriesResultBO listProjects(OrgFileListParamsBO params) {
        Page<Project> page = projectMapper.selectPage(
                new Page<>(params.getPageNum(), params.getPageSize()),
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getOrgId, params.getOrgId())
                        .orderByAsc(Project::getTitle)
                        .orderByAsc(Project::getId));
        List<OrgFileEntryResultBO> entries = page.getRecords().stream()
                .map(project -> OrgFileEntryResultBO.builder()
                        .id(project.getId().toString())
                        .kind(OrgFileEntryKind.FOLDER)
                        .name(project.getTitle())
                        .build())
                .toList();
        return OrgFileEntriesResultBO.builder().total(page.getTotal()).entries(entries).build();
    }

    private OrgFileEntriesResultBO listItems(OrgFileListParamsBO params) {
        Project project = requireProject(params.getProjectId(), params.getOrgId());
        Page<Item> page = itemMapper.selectPage(
                new Page<>(params.getPageNum(), params.getPageSize()),
                new LambdaQueryWrapper<Item>()
                        .eq(Item::getProjectId, project.getId())
                        .orderByAsc(Item::getSort)
                        .orderByAsc(Item::getId));
        List<OrgFileEntryResultBO> entries = page.getRecords().stream()
                .map(item -> OrgFileEntryResultBO.builder()
                        .id(item.getId().toString())
                        .kind(OrgFileEntryKind.FOLDER)
                        .name(item.getTitle())
                        .build())
                .toList();
        return OrgFileEntriesResultBO.builder()
                .projectId(project.getId())
                .projectTitle(project.getTitle())
                .total(page.getTotal())
                .entries(entries)
                .build();
    }

    private OrgFileEntriesResultBO listFiles(OrgFileListParamsBO params) {
        Item item = requireItem(params.getItemId(), params.getProjectId(), params.getOrgId());
        Project project = requireProject(item.getProjectId(), params.getOrgId());
        List<OrgFileEntryResultBO> entries = fileEntries(item.getId(), params.getOrgId());
        return OrgFileEntriesResultBO.builder()
                .projectId(project.getId())
                .projectTitle(project.getTitle())
                .itemId(item.getId())
                .itemTitle(item.getTitle())
                .total((long) entries.size())
                .entries(entries)
                .build();
    }

    private List<OrgFileEntryResultBO> fileEntries(Long itemId, Long orgId) {
        List<ItemTaskNode> nodes = itemTaskNodeMapper.selectList(new LambdaQueryWrapper<ItemTaskNode>()
                .eq(ItemTaskNode::getItemId, itemId)
                .orderByAsc(ItemTaskNode::getSort)
                .orderByAsc(ItemTaskNode::getParallelSort)
                .orderByAsc(ItemTaskNode::getId));
        Map<Long, List<TaskInstance>> instancesByNode = instancesByNode(itemId);
        Map<Long, TaskSubmission> currentByInstance = currentSubmissions(itemId);
        Map<Long, String> nicknames = submitterNames(currentByInstance.values());
        List<OrgFileEntryResultBO> entries = new ArrayList<>();
        for (ItemTaskNode node : nodes) {
            List<TaskInstance> instances = instancesByNode.getOrDefault(node.getId(), List.of());
            for (TaskInstance instance : instances) {
                TaskSubmission submission = currentByInstance.get(instance.getId());
                if (submission == null) {
                    continue;
                }
                entries.addAll(filesOf(node, instance, submission, orgId, nicknames));
            }
        }
        return entries;
    }

    private List<Long> currentFileIds(TaskInstance instance) {
        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        TaskSubmission submission = currentSubmissions(instance.getItemId()).get(instance.getId());
        if (node == null || submission == null) {
            return List.of();
        }
        List<TaskFormField> fields = readFields(node.getMetaSchema());
        TaskFormService.ValidatedValues values = readValues(fields, submission.getMetadata());
        if (fields == null || values == null) {
            return List.of();
        }
        List<Long> fileIds = new ArrayList<>();
        for (TaskFormField field : fields) {
            Long fileId = fileId(field, values);
            if (fileId != null) {
                fileIds.add(fileId);
            }
        }
        return fileIds;
    }

    private List<OrgFileEntryResultBO> filesOf(
            ItemTaskNode node,
            TaskInstance instance,
            TaskSubmission submission,
            Long orgId,
            Map<Long, String> nicknames) {
        List<TaskFormField> fields = readFields(node.getMetaSchema());
        TaskFormService.ValidatedValues values = fields == null ? null : readValues(fields, submission.getMetadata());
        if (fields == null || values == null) {
            return List.of();
        }
        List<Long> fileIds = new ArrayList<>();
        for (TaskFormField field : fields) {
            Long fileId = fileId(field, values);
            if (fileId != null) {
                fileIds.add(fileId);
            }
        }
        Map<Long, ActivatedResource> resources = fileIds.isEmpty()
                ? Map.of()
                : resourceService.findActivated(TASK_SUBMIT, instance.getId(), orgId, fileIds);
        Set<Long> deprecatedIds = resourceDeprecationService.deprecatedIds(fileIds);
        String submitterName = nicknames.getOrDefault(submission.getSubmitterId(), "");
        List<OrgFileEntryResultBO> entries = new ArrayList<>();
        for (TaskFormField field : fields) {
            Long fileId = fileId(field, values);
            if (fileId == null || deprecatedIds.contains(fileId)) {
                continue;
            }
            entries.add(fileEntry(node, instance, submission, field, fileId,
                    resources.get(fileId), submitterName));
        }
        return entries;
    }

    private OrgFileEntryResultBO fileEntry(
            ItemTaskNode node,
            TaskInstance instance,
            TaskSubmission submission,
            TaskFormField field,
            Long fileId,
            ActivatedResource resource,
            String submitterName) {
        boolean available = resource != null;
        return OrgFileEntryResultBO.builder()
                .id(instance.getId() + ":" + field.key())
                .kind(OrgFileEntryKind.FILE)
                .name(available ? resource.fileName() : field.label())
                .taskInstanceId(instance.getId())
                .taskName(node.getName())
                .fieldKey(field.key())
                .fieldLabel(field.label())
                .fileId(fileId)
                .fileSize(available ? resource.fileSize() : null)
                .fileMime(available ? resource.fileMime() : null)
                .availability(available ? AVAILABLE : UNAVAILABLE)
                .submitterName(submitterName)
                .submittedAt(submission.getCreatedAt())
                .build();
    }

    private Map<Long, List<TaskInstance>> instancesByNode(Long itemId) {
        List<TaskInstance> instances = taskInstanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getItemId, itemId)
                .orderByAsc(TaskInstance::getId));
        Map<Long, List<TaskInstance>> byNode = new HashMap<>();
        for (TaskInstance instance : instances) {
            byNode.computeIfAbsent(instance.getItemTaskNodeId(), key -> new ArrayList<>()).add(instance);
        }
        return byNode;
    }

    private Map<Long, TaskSubmission> currentSubmissions(Long itemId) {
        List<TaskSubmission> submissions = taskSubmissionMapper.selectList(new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getItemId, itemId)
                .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED));
        Map<Long, TaskSubmission> current = new HashMap<>();
        for (TaskSubmission submission : submissions) {
            current.merge(submission.getTaskInstanceId(), submission, this::laterSubmission);
        }
        return current;
    }

    private TaskSubmission laterSubmission(TaskSubmission left, TaskSubmission right) {
        return submissionOrder().compare(left, right) >= 0 ? left : right;
    }

    private Comparator<TaskSubmission> submissionOrder() {
        return Comparator.comparing(TaskSubmission::getVersionNo, Comparator.nullsFirst(Integer::compareTo))
                .thenComparing(TaskSubmission::getCreatedAt, Comparator.nullsFirst(LocalDateTime::compareTo))
                .thenComparing(TaskSubmission::getId, Comparator.nullsFirst(Long::compareTo));
    }

    private Map<Long, String> submitterNames(Iterable<TaskSubmission> submissions) {
        List<Long> userIds = new ArrayList<>();
        for (TaskSubmission submission : submissions) {
            if (submission.getSubmitterId() != null && !userIds.contains(submission.getSubmitterId())) {
                userIds.add(submission.getSubmitterId());
            }
        }
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (User user : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))) {
            names.put(user.getId(), user.getNickname() == null ? "" : user.getNickname());
        }
        return names;
    }

    private Long fileId(TaskFormField field, TaskFormService.ValidatedValues values) {
        if (!"file".equals(field.type())) {
            return null;
        }
        Object value = values.values().get(field.key());
        if (!(value instanceof Map<?, ?> file) || !(file.get("fileId") instanceof String text)) {
            return null;
        }
        try {
            return taskFormService.parseFileId(text);
        } catch (AppException exception) {
            return null;
        }
    }

    private List<TaskFormField> readFields(String schema) {
        if (schema == null || schema.isBlank()) {
            return List.of();
        }
        try {
            return taskFormService.parseFields(schema);
        } catch (AppException exception) {
            return null;
        }
    }

    private TaskFormService.ValidatedValues readValues(List<TaskFormField> fields, String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        try {
            return taskFormService.parseValues(fields, metadata);
        } catch (AppException exception) {
            return null;
        }
    }

    private Project requireProject(Long projectId, Long orgId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Objects.equals(project.getOrgId(), orgId)) {
            throw missing("企划不存在");
        }
        return project;
    }

    private Item requireItem(Long itemId, Long projectId, Long orgId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw missing("项目不存在");
        }
        Project project = requireProject(item.getProjectId(), orgId);
        if (projectId != null && !projectId.equals(project.getId())) {
            throw missing("项目不存在");
        }
        return item;
    }

    private AppException missing(String message) {
        return new AppException(ResultCode.DATA_NOT_EXIT, message);
    }
}
