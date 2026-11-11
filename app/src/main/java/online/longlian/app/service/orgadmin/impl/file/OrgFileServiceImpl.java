package online.longlian.app.service.orgadmin.impl.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.ActivatedResourceRead;
import online.longlian.app.pojo.bo.common.TaskFormField;
import online.longlian.app.pojo.bo.orgadmin.OrgFileExpansionParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileExpansionResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileBatchFileResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileBatchTargetParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDiscardParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDownloadParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileEntriesResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileEntryResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileTypeItemBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.service.orgadmin.OrgFileService;
import online.longlian.app.service.resource.ResourceDeprecationService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.OrgFileEntryKind;
import online.longlian.common.enumeration.OrgFileView;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private static final int TYPE_ITEM_BATCH = 100;
    private static final Comparator<ItemTaskNode> NODE_ORDER = Comparator
            .comparing(ItemTaskNode::getSort, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(ItemTaskNode::getParallelSort, Comparator.nullsFirst(Integer::compareTo))
            .thenComparing(ItemTaskNode::getId, Comparator.nullsLast(Long::compareTo));

    private final ProjectMapper projectMapper;
    private final ProjectTypeMapper projectTypeMapper;
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
        OrgFileView view = params.getView() == null ? OrgFileView.item : params.getView();
        if (params.getProjectId() == null) {
            if (params.getItemId() != null || params.getTaskId() != null) {
                throw missing("企划不存在");
            }
            if (params.getTypeId() == null) {
                return listTypes(params);
            }
            return listProjects(params, requireType(params.getTypeId(), params.getOrgId()));
        }
        Project project = requireProject(params.getProjectId(), params.getOrgId());
        ProjectType type = typeOf(project, params.getTypeId());
        if (view == OrgFileView.task) {
            if (params.getTaskId() != null) {
                return listTaskFiles(params, type, project);
            }
            return listTaskFolders(params, type, project);
        }
        if (params.getItemId() != null) {
            return listFiles(params, type);
        }
        return listItems(params, type);
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
    public OrgFileExpansionResultBO expand(OrgFileExpansionParamsBO params) {
        if (params.getTargets() == null || params.getTargets().isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, "请选择要展开的文件");
        }
        if (params.getTargets().size() > 100) {
            throw new AppException(ResultCode.PARAM_ERROR, "一次最多选择 100 项");
        }
        LinkedHashMap<String, Downloadable> ordered = new LinkedHashMap<>();
        for (OrgFileBatchTargetParamsBO target : params.getTargets()) {
            for (Downloadable file : resolveTarget(params.getOrgId(), target)) {
                ordered.putIfAbsent(downloadKey(file), file);
            }
        }
        List<OrgFileBatchFileResultBO> files = new ArrayList<>();
        for (Downloadable file : ordered.values()) {
            files.add(OrgFileBatchFileResultBO.builder()
                    .taskInstanceId(file.taskInstanceId())
                    .fieldKey(file.fieldKey())
                    .fileId(file.fileId())
                    .fileName(file.resource().fileName())
                    .fileSize(file.resource().fileSize())
                    .typeName(file.typeName())
                    .projectTitle(file.projectTitle())
                    .itemTitle(file.itemTitle())
                    .taskName(file.taskName())
                    .fieldLabel(file.fieldLabel())
                    .versionNo(file.versionNo())
                    .build());
        }
        return OrgFileExpansionResultBO.builder().files(files).build();
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
        requireCurrentField(instance, params.getFieldKey(), params.getFileId());
        // 不改提交 JSON。必填文件被清空会让任务详情无法读取；弃置后详情和历史版本显示为不可用。
        boolean deprecated = resourceDeprecationService.deprecateTaskSubmitFile(
                params.getFileId(), instance.getId(), params.getOrgId(), LocalDateTime.now(clock));
        if (!deprecated) {
            throw new AppException(ResultCode.OPERATION_FAIL, "文件仍被其他资料使用，不能删除");
        }
    }

    private OrgFileEntriesResultBO listTaskFolders(
            OrgFileListParamsBO params, ProjectType type, Project project) {
        List<TaskGroup> groups = taskGroups(itemsOf(project));
        int from = (params.getPageNum() - 1) * params.getPageSize();
        int to = Math.min(from + params.getPageSize(), groups.size());
        List<OrgFileEntryResultBO> entries = new ArrayList<>();
        if (from < groups.size()) {
            for (TaskGroup group : groups.subList(from, to)) {
                entries.add(OrgFileEntryResultBO.builder()
                        .id(group.baseTaskId().toString())
                        .kind(OrgFileEntryKind.FOLDER)
                        .name(group.name())
                        .build());
            }
        }
        return folder(type, project, null, groups.size(), entries);
    }

    private OrgFileEntriesResultBO listTaskFiles(
            OrgFileListParamsBO params, ProjectType type, Project project) {
        List<Item> items = itemsOf(project);
        TaskGroup group = taskGroups(items).stream()
                .filter(candidate -> candidate.baseTaskId().equals(params.getTaskId()))
                .findFirst()
                .orElseThrow(() -> missing("任务不存在"));
        List<OrgFileTypeItemBO> rows = new ArrayList<>();
        for (Item item : items) {
            rows.add(new OrgFileTypeItemBO(item.getId(), item.getTitle(), project.getId(), project.getTitle()));
        }
        List<PendingFile> pending = rows.isEmpty()
                ? List.of()
                : pendingFiles(rows, params.getOrgId(), params.getTaskId());
        long pageStart = (long) (params.getPageNum() - 1) * params.getPageSize();
        long pageEnd = pageStart + params.getPageSize();
        List<OrgFileEntryResultBO> page = new ArrayList<>();
        appendPage(pending, 0, pageStart, pageEnd, page, params.getOrgId());
        OrgFileEntriesResultBO result = folder(type, project, null, pending.size(), page);
        result.setTaskId(group.baseTaskId());
        result.setTaskName(group.name());
        return result;
    }

    private List<Item> itemsOf(Project project) {
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getProjectId, project.getId())
                .orderByAsc(Item::getSort)
                .orderByAsc(Item::getId));
    }

    private List<ItemTaskNode> nodesOf(List<Item> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = items.stream().map(Item::getId).toList();
        return itemTaskNodeMapper.selectList(new LambdaQueryWrapper<ItemTaskNode>()
                .in(ItemTaskNode::getItemId, itemIds)
                .orderByAsc(ItemTaskNode::getSort)
                .orderByAsc(ItemTaskNode::getParallelSort)
                .orderByAsc(ItemTaskNode::getId));
    }

    /**
     * 任务目录来自企划里仍在的项目任务节点。原子任务后来被删除或停用时，节点仍保留当时的标识和名称。
     */
    private List<TaskGroup> taskGroups(List<Item> items) {
        List<ItemTaskNode> nodes = new ArrayList<>(nodesOf(items));
        nodes.sort(NODE_ORDER);
        Map<Long, TaskGroup> groups = new LinkedHashMap<>();
        for (ItemTaskNode node : nodes) {
            if (node.getBaseTaskId() == null) {
                continue;
            }
            groups.computeIfAbsent(node.getBaseTaskId(), id -> new TaskGroup(id, taskFolderName(node)));
        }
        return List.copyOf(groups.values());
    }

    private static String taskFolderName(ItemTaskNode node) {
        String name = node.getName();
        if (name == null || name.isBlank()) {
            return "未命名";
        }
        return name;
    }

    private ProjectType typeOf(Project project, Long requestedTypeId) {
        if (requestedTypeId != null && !requestedTypeId.equals(project.getTypeId())) {
            throw missing("企划不存在");
        }
        if (project.getTypeId() == null) {
            return null;
        }
        ProjectType type = projectTypeMapper.selectById(project.getTypeId());
        if (type == null || !Objects.equals(type.getOrgId(), project.getOrgId())) {
            return null;
        }
        return type;
    }

    private OrgFileEntriesResultBO listTypes(OrgFileListParamsBO params) {
        Page<ProjectType> page = projectTypeMapper.selectPage(
                new Page<>(params.getPageNum(), params.getPageSize()),
                new LambdaQueryWrapper<ProjectType>()
                        .eq(ProjectType::getOrgId, params.getOrgId())
                        .orderByDesc(ProjectType::getPriority)
                        .orderByAsc(ProjectType::getName)
                        .orderByAsc(ProjectType::getId));
        List<OrgFileEntryResultBO> entries = page.getRecords().stream()
                .map(type -> OrgFileEntryResultBO.builder()
                        .id(type.getId().toString())
                        .kind(OrgFileEntryKind.FOLDER)
                        .name(type.getName())
                        .build())
                .toList();
        return OrgFileEntriesResultBO.builder().total(page.getTotal()).entries(entries).build();
    }

    private OrgFileEntriesResultBO listProjects(OrgFileListParamsBO params, ProjectType type) {
        LambdaQueryWrapper<Project> query = new LambdaQueryWrapper<Project>()
                .eq(Project::getOrgId, params.getOrgId());
        if (type != null) {
            query.eq(Project::getTypeId, type.getId());
        }
        Page<Project> page = projectMapper.selectPage(
                new Page<>(params.getPageNum(), params.getPageSize()),
                query.orderByAsc(Project::getTitle).orderByAsc(Project::getId));
        List<OrgFileEntryResultBO> entries = page.getRecords().stream()
                .map(project -> OrgFileEntryResultBO.builder()
                        .id(project.getId().toString())
                        .kind(OrgFileEntryKind.FOLDER)
                        .name(project.getTitle())
                        .build())
                .toList();
        return folder(type, null, null, page.getTotal(), entries);
    }

    private OrgFileEntriesResultBO listItems(OrgFileListParamsBO params, ProjectType type) {
        Project project = requireProject(params.getProjectId(), params.getOrgId());
        if (type != null && !type.getId().equals(project.getTypeId())) {
            throw missing("企划不存在");
        }
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
        return folder(type, project, null, page.getTotal(), entries);
    }


    private OrgFileEntriesResultBO listFiles(OrgFileListParamsBO params, ProjectType type) {
        Item item = requireItem(params.getItemId(), params.getProjectId(), params.getOrgId());
        Project project = requireProject(item.getProjectId(), params.getOrgId());
        if (type != null && !type.getId().equals(project.getTypeId())) {
            throw missing("企划不存在");
        }
        List<OrgFileEntryResultBO> entries = fileEntries(item.getId(), params.getOrgId());
        return folder(type, project, item, entries.size(), entries);
    }

    private OrgFileEntriesResultBO folder(
            ProjectType type,
            Project project,
            Item item,
            long total,
            List<OrgFileEntryResultBO> entries) {
        return OrgFileEntriesResultBO.builder()
                .typeId(type == null ? null : type.getId())
                .typeName(type == null ? null : type.getName())
                .projectId(project == null ? null : project.getId())
                .projectTitle(project == null ? null : project.getTitle())
                .itemId(item == null ? null : item.getId())
                .itemTitle(item == null ? null : item.getTitle())
                .total(total)
                .entries(entries)
                .build();
    }


    private ProjectType requireType(Long typeId, Long orgId) {
        ProjectType type = projectTypeMapper.selectById(typeId);
        if (type == null || !Objects.equals(type.getOrgId(), orgId)) {
            throw missing("企划类型不存在");
        }
        return type;
    }

    private List<OrgFileEntryResultBO> fileEntries(Long itemId, Long orgId) {
        List<ItemTaskNode> nodes = itemTaskNodeMapper.selectList(new LambdaQueryWrapper<ItemTaskNode>()
                .eq(ItemTaskNode::getItemId, itemId)
                .orderByAsc(ItemTaskNode::getSort)
                .orderByAsc(ItemTaskNode::getParallelSort)
                .orderByAsc(ItemTaskNode::getId));
        Map<Long, List<TaskInstance>> instancesByNode = instancesByNode(itemId);
        List<TaskSubmission> submissions = listedSubmissions(itemId);
        Map<Long, List<TaskSubmission>> versions = versionsByInstance(submissions);
        Map<Long, String> nicknames = submitterNames(submissions);
        List<OrgFileEntryResultBO> entries = new ArrayList<>();
        for (ItemTaskNode node : nodes) {
            for (TaskInstance instance : instancesByNode.getOrDefault(node.getId(), List.of())) {
                for (TaskSubmission submission : versions.getOrDefault(instance.getId(), List.of())) {
                    entries.addAll(filesOf(node, instance, submission, orgId, nicknames));
                }
            }
        }
        return entries;
    }

    private List<Long> currentFileIds(TaskInstance instance) {
        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        if (node == null) {
            return List.of();
        }
        List<TaskFormField> fields = readFields(node.getMetaSchema());
        if (fields == null) {
            return List.of();
        }
        List<Long> fileIds = new ArrayList<>();
        for (TaskSubmission submission : submissionsOf(instance.getId())) {
            TaskFormService.ValidatedValues values = readValues(fields, submission.getMetadata());
            if (values == null) {
                continue;
            }
            for (TaskFormField field : fields) {
                Long fileId = fileId(field, values);
                if (fileId != null) {
                    fileIds.add(fileId);
                }
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
                .id(instance.getId() + ":" + submission.getId() + ":" + field.key())
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
                .versionNo(submission.getVersionNo())
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

    private static final List<TaskSubmissionStatus> LISTED_STATUSES = List.of(
            TaskSubmissionStatus.SUBMITTED, TaskSubmissionStatus.REJECTED, TaskSubmissionStatus.RESET);

    private List<TaskSubmission> listedSubmissions(Long itemId) {
        return taskSubmissionMapper.selectList(new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getItemId, itemId)
                .in(TaskSubmission::getStatus, LISTED_STATUSES));
    }

    private List<TaskSubmission> submissionsOf(Long instanceId) {
        List<TaskSubmission> submissions = new ArrayList<>(taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskInstanceId, instanceId)
                        .in(TaskSubmission::getStatus, LISTED_STATUSES)));
        submissions.sort(submissionOrder().reversed());
        return submissions;
    }

    private Map<Long, List<TaskSubmission>> versionsByInstance(List<TaskSubmission> submissions) {
        Map<Long, List<TaskSubmission>> grouped = new HashMap<>();
        for (TaskSubmission submission : submissions) {
            grouped.computeIfAbsent(submission.getTaskInstanceId(), key -> new ArrayList<>()).add(submission);
        }
        for (List<TaskSubmission> versions : grouped.values()) {
            versions.sort(submissionOrder().reversed());
        }
        return grouped;
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
            return List.of();
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

    private String typeName(Project project) {
        if (project.getTypeId() == null) {
            return null;
        }
        ProjectType type = projectTypeMapper.selectById(project.getTypeId());
        if (type == null || !Objects.equals(type.getOrgId(), project.getOrgId())) {
            return null;
        }
        return type.getName();
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

    private List<Downloadable> resolveTarget(Long orgId, OrgFileBatchTargetParamsBO target) {
        if (target == null || target.getKind() == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "展开目标不能为空");
        }
        return switch (target.getKind()) {
            case FILE -> resolveFile(orgId, target);
            case ITEM -> resolveItem(orgId, target.getItemId());
            case PROJECT -> resolveProject(orgId, target.getProjectId());
            case TYPE -> resolveType(orgId, target.getTypeId());
            case TASK -> resolveTask(orgId, target.getProjectId(), target.getTaskId());
        };
    }

    private List<Downloadable> resolveFile(Long orgId, OrgFileBatchTargetParamsBO target) {
        requireFileTarget(target);
        TaskInstance instance = taskInstanceMapper.selectById(target.getTaskInstanceId());
        if (instance == null) {
            throw missing("附件不存在");
        }
        Item item = requireItem(instance.getItemId(), instance.getProjectId(), orgId);
        Project project = requireProject(item.getProjectId(), orgId);
        CurrentFile current = requireCurrentField(instance, target.getFieldKey(), target.getFileId());
        if (resourceDeprecationService.deprecatedIds(List.of(target.getFileId())).contains(target.getFileId())) {
            return List.of();
        }
        ActivatedResource resource = resourceService
                .findActivated(TASK_SUBMIT, instance.getId(), orgId, List.of(target.getFileId()))
                .get(target.getFileId());
        if (resource == null) {
            return List.of();
        }
        return List.of(new Downloadable(
                instance.getId(), current.field().key(), target.getFileId(), resource,
                typeName(project), project.getTitle(), item.getTitle(), current.taskName(),
                current.field().label(), current.versionNo()));
    }

    private static void requireFileTarget(OrgFileBatchTargetParamsBO target) {
        if (target.getTaskInstanceId() == null || target.getFileId() == null
                || target.getFieldKey() == null || target.getFieldKey().isBlank()) {
            throw new AppException(ResultCode.PARAM_ERROR, "文件参数不完整");
        }
    }

    private CurrentFile requireCurrentField(TaskInstance instance, String fieldKey, Long fileId) {
        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        if (node == null) {
            throw missing("附件不存在");
        }
        List<TaskFormField> fields = readFields(node.getMetaSchema());
        if (fields == null) {
            throw missing("附件不存在");
        }
        for (TaskSubmission submission : submissionsOf(instance.getId())) {
            TaskFormService.ValidatedValues values = readValues(fields, submission.getMetadata());
            if (values == null) {
                continue;
            }
            for (TaskFormField field : fields) {
                if (fieldKey.equals(field.key()) && fileId.equals(fileId(field, values))) {
                    return new CurrentFile(field, node.getName(), submission.getVersionNo());
                }
            }
        }
        throw missing("附件不存在");
    }

    private List<Downloadable> resolveItem(Long orgId, Long itemId) {
        if (itemId == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "项目不能为空");
        }
        Item item = requireItem(itemId, null, orgId);
        Project project = requireProject(item.getProjectId(), orgId);
        return downloadables(List.of(new OrgFileTypeItemBO(
                item.getId(), item.getTitle(), project.getId(), project.getTitle())), orgId, typeName(project));
    }

    private List<Downloadable> resolveProject(Long orgId, Long projectId) {
        if (projectId == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "企划不能为空");
        }
        Project project = requireProject(projectId, orgId);
        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getProjectId, project.getId())
                .orderByAsc(Item::getSort)
                .orderByAsc(Item::getId));
        List<OrgFileTypeItemBO> rows = new ArrayList<>();
        for (Item item : items) {
            rows.add(new OrgFileTypeItemBO(item.getId(), item.getTitle(), project.getId(), project.getTitle()));
        }
        return downloadables(rows, orgId, typeName(project));
    }

    private List<Downloadable> resolveTask(Long orgId, Long projectId, Long taskId) {
        if (projectId == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "企划不能为空");
        }
        if (taskId == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "任务不能为空");
        }
        Project project = requireProject(projectId, orgId);
        List<Item> items = itemsOf(project);
        if (taskGroups(items).stream().noneMatch(group -> taskId.equals(group.baseTaskId()))) {
            throw missing("任务不存在");
        }
        List<OrgFileTypeItemBO> rows = new ArrayList<>();
        for (Item item : items) {
            rows.add(new OrgFileTypeItemBO(item.getId(), item.getTitle(), project.getId(), project.getTitle()));
        }
        return downloadables(rows, orgId, typeName(project), taskId);
    }


    private List<Downloadable> resolveType(Long orgId, Long typeId) {
        if (typeId == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "企划类型不能为空");
        }
        ProjectType type = requireType(typeId, orgId);
        List<Downloadable> files = new ArrayList<>();
        long offset = 0;
        while (true) {
            List<OrgFileTypeItemBO> items = itemMapper.selectByProjectType(
                    orgId, type.getId(), offset, TYPE_ITEM_BATCH);
            if (items == null || items.isEmpty()) {
                break;
            }
            files.addAll(downloadables(items, orgId, type.getName()));
            if (items.size() < TYPE_ITEM_BATCH) {
                break;
            }
            offset += items.size();
        }
        return files;
    }

    private List<Downloadable> downloadables(List<OrgFileTypeItemBO> items, Long orgId, String typeName) {
        return downloadables(items, orgId, typeName, null);
    }

    private List<Downloadable> downloadables(
            List<OrgFileTypeItemBO> items, Long orgId, String typeName, Long baseTaskId) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<PendingFile> pending = pendingFiles(items, orgId, baseTaskId);
        Map<Long, List<PendingFile>> byInstance = new LinkedHashMap<>();
        for (PendingFile file : pending) {
            byInstance.computeIfAbsent(file.instance().getId(), key -> new ArrayList<>()).add(file);
        }
        Map<Long, Map<Long, ActivatedResource>> resourcesByInstance = new HashMap<>();
        for (Map.Entry<Long, List<PendingFile>> entry : byInstance.entrySet()) {
            List<Long> ids = entry.getValue().stream().map(PendingFile::fileId).distinct().toList();
            resourcesByInstance.put(entry.getKey(),
                    resourceService.findActivated(TASK_SUBMIT, entry.getKey(), orgId, ids));
        }
        List<Downloadable> files = new ArrayList<>();
        for (PendingFile file : pending) {
            ActivatedResource resource = resourcesByInstance
                    .getOrDefault(file.instance().getId(), Map.of())
                    .get(file.fileId());
            if (resource == null) {
                continue;
            }
            files.add(new Downloadable(
                    file.instance().getId(), file.field().key(), file.fileId(), resource,
                    typeName, file.projectTitle(), file.itemTitle(), file.node().getName(),
                    file.field().label(), file.submission().getVersionNo()));
        }
        return files;
    }

    private static String downloadKey(Downloadable file) {
        return file.taskInstanceId() + "\u001f" + file.fieldKey() + "\u001f" + file.fileId();
    }

    private List<PendingFile> pendingFiles(List<OrgFileTypeItemBO> items, Long orgId, Long baseTaskId) {
        List<Long> itemIds = items.stream().map(OrgFileTypeItemBO::getItemId).toList();
        Map<Long, List<ItemTaskNode>> nodesByItem = new HashMap<>();
        for (ItemTaskNode node : itemTaskNodeMapper.selectList(new LambdaQueryWrapper<ItemTaskNode>()
                .in(ItemTaskNode::getItemId, itemIds)
                .orderByAsc(ItemTaskNode::getSort)
                .orderByAsc(ItemTaskNode::getParallelSort)
                .orderByAsc(ItemTaskNode::getId))) {
            nodesByItem.computeIfAbsent(node.getItemId(), key -> new ArrayList<>()).add(node);
        }
        Map<Long, List<TaskInstance>> instancesByNode = new HashMap<>();
        for (TaskInstance instance : taskInstanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .in(TaskInstance::getItemId, itemIds)
                .orderByAsc(TaskInstance::getId))) {
            instancesByNode.computeIfAbsent(instance.getItemTaskNodeId(), key -> new ArrayList<>()).add(instance);
        }
        List<TaskSubmission> submissions = taskSubmissionMapper.selectList(new LambdaQueryWrapper<TaskSubmission>()
                .in(TaskSubmission::getItemId, itemIds)
                .in(TaskSubmission::getStatus, LISTED_STATUSES));
        Map<Long, List<TaskSubmission>> versions = versionsByInstance(submissions);
        Map<Long, String> nicknames = submitterNames(submissions);
        List<Long> fileIds = new ArrayList<>();
        List<PendingFile> pending = new ArrayList<>();
        for (OrgFileTypeItemBO item : items) {
            String subtitle = baseTaskId == null
                    ? item.getProjectTitle() + " · " + item.getItemTitle()
                    : item.getItemTitle();
            for (ItemTaskNode node : nodesByItem.getOrDefault(item.getItemId(), List.of())) {
                if (baseTaskId != null && !baseTaskId.equals(node.getBaseTaskId())) {
                    continue;
                }
                for (TaskInstance instance : instancesByNode.getOrDefault(node.getId(), List.of())) {
                    for (TaskSubmission submission : versions.getOrDefault(instance.getId(), List.of())) {
                        List<TaskFormField> fields = readFields(node.getMetaSchema());
                        TaskFormService.ValidatedValues values = fields == null
                                ? null : readValues(fields, submission.getMetadata());
                        if (fields == null || values == null) {
                            continue;
                        }
                        String submitterName = nicknames.getOrDefault(submission.getSubmitterId(), "");
                        for (TaskFormField field : fields) {
                            Long fileId = fileId(field, values);
                            if (fileId == null) {
                                continue;
                            }
                            fileIds.add(fileId);
                            pending.add(new PendingFile(
                                    node, instance, submission, field, fileId, submitterName, subtitle,
                                    item.getProjectTitle(), item.getItemTitle()));
                        }
                    }
                }
            }
        }
        Set<Long> deprecatedIds = resourceDeprecationService.deprecatedIds(fileIds);
        if (deprecatedIds.isEmpty()) {
            return pending;
        }
        return pending.stream().filter(file -> !deprecatedIds.contains(file.fileId())).toList();
    }

    private void appendPage(
            List<PendingFile> pending,
            long batchStart,
            long pageStart,
            long pageEnd,
            List<OrgFileEntryResultBO> page,
            Long orgId) {
        List<PendingFile> chosen = new ArrayList<>();
        for (int index = 0; index < pending.size(); index++) {
            long position = batchStart + index;
            if (position >= pageStart && position < pageEnd) {
                chosen.add(pending.get(index));
            }
        }
        if (chosen.isEmpty()) {
            return;
        }
        Map<Long, List<PendingFile>> byInstance = new LinkedHashMap<>();
        for (PendingFile file : chosen) {
            byInstance.computeIfAbsent(file.instance().getId(), key -> new ArrayList<>()).add(file);
        }
        Map<Long, ActivatedResource> resources = new HashMap<>();
        for (Map.Entry<Long, List<PendingFile>> entry : byInstance.entrySet()) {
            List<Long> ids = entry.getValue().stream().map(PendingFile::fileId).toList();
            resources.putAll(resourceService.findActivated(TASK_SUBMIT, entry.getKey(), orgId, ids));
        }
        for (PendingFile file : chosen) {
            OrgFileEntryResultBO entry = fileEntry(file.node(), file.instance(), file.submission(), file.field(),
                    file.fileId(), resources.get(file.fileId()), file.submitterName());
            entry.setSubtitle(file.subtitle());
            page.add(entry);
        }
    }

    private record PendingFile(
            ItemTaskNode node,
            TaskInstance instance,
            TaskSubmission submission,
            TaskFormField field,
            Long fileId,
            String submitterName,
            String subtitle,
            String projectTitle,
            String itemTitle) {
    }

    private record Downloadable(
            Long taskInstanceId,
            String fieldKey,
            Long fileId,
            ActivatedResource resource,
            String typeName,
            String projectTitle,
            String itemTitle,
            String taskName,
            String fieldLabel,
            Integer versionNo) {
    }

    private record CurrentFile(TaskFormField field, String taskName, Integer versionNo) {
    }

    private record TaskGroup(Long baseTaskId, String name) {
    }
}
