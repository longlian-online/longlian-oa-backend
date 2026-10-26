package online.longlian.app.service.app.impl.item;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.ItemOrderEntryResultBO;
import online.longlian.app.pojo.bo.app.ItemOrderQueryParamsBO;
import online.longlian.app.pojo.bo.app.ItemReorderParamsBO;
import online.longlian.app.pojo.bo.app.ItemSortUpdateBO;
import online.longlian.app.pojo.bo.app.ProjectAccessParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;
import online.longlian.app.pojo.bo.common.ResourceAbandonParamsBO;
import online.longlian.app.service.app.ItemService;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskInstanceStatus;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ProjectMapper projectMapper;
    private final ItemMapper itemMapper;
    private final ItemTaskFlowMapper itemTaskFlowMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final BaseTaskMapper baseTaskMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final ResourceService resourceService;
    private final Clock clock;
    private final ItemQueryBuilder itemQueryBuilder;
    private final ItemAssembler itemAssembler;
    private final ProjectService projectService;

    @Override
    public PageResultBO<ProjectItemListVO> listProjectItems(ItemListParamsBO params) {
        Project project = projectMapper.selectById(params.getProjectId());
        if (project == null || !project.getOrgId().equals(params.getOrgId())
                || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT);
        }

        Page<Item> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<Item> itemPage = itemMapper.selectPage(page, itemQueryBuilder.buildListQuery(params));
        List<Item> items = itemPage.getRecords();
        if (items.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), itemPage.getTotal());
        }
        List<ProjectItemListVO> list = itemAssembler.assembleList(items);
        markSubmittedTasks(list);
        return new PageResultBO<>(list, itemPage.getTotal());
    }

    @Override
    public List<ItemOrderEntryResultBO> getProjectItemOrder(ItemOrderQueryParamsBO params) {
        checkProjectCreator(params.getProjectId(), params.getOperatorId(), params.getOrgId());
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .select(Item::getId, Item::getTitle)
                        .eq(Item::getProjectId, params.getProjectId())
                        .orderByAsc(Item::getSort, Item::getId)).stream()
                .map(item -> new ItemOrderEntryResultBO(item.getId(), item.getTitle()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderProjectItems(ItemReorderParamsBO params) {
        lockProjectCreator(params.getProjectId(), params.getOperatorId(), params.getOrgId());
        List<Long> expected = params.getExpectedItemIds();
        List<Long> requested = params.getItemIds();
        Set<Long> expectedSet = new HashSet<>(expected);
        Set<Long> requestedSet = new HashSet<>(requested);
        if (expectedSet.size() != expected.size() || requestedSet.size() != requested.size()
                || !expectedSet.equals(requestedSet)) {
            throw new AppException(ResultCode.PARAM_ERROR, "排序前后必须包含相同且不重复的项目");
        }

        // 锁定当前读，避免复用外层事务较早建立的 MVCC 快照。
        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .select(Item::getId, Item::getSort)
                .eq(Item::getProjectId, params.getProjectId())
                .orderByAsc(Item::getId)
                .last("FOR UPDATE"));
        if (items.size() != expectedSet.size()
                || items.stream().anyMatch(item -> !expectedSet.contains(item.getId()))) {
            throw orderConflict();
        }
        List<Long> current = items.stream()
                .sorted(Comparator.comparing(Item::getSort).thenComparing(Item::getId))
                .map(Item::getId).toList();
        if (current.equals(requested)) {
            return;
        }
        if (!current.equals(expected)) {
            throw orderConflict();
        }

        Map<Long, Long> currentSorts = new HashMap<>();
        items.forEach(item -> currentSorts.put(item.getId(), item.getSort()));
        List<ItemSortUpdateBO> updates = new ArrayList<>();
        for (int index = 0; index < requested.size(); index++) {
            Long id = requested.get(index);
            long sort = index + 1L;
            if (currentSorts.get(id) != sort) {
                updates.add(new ItemSortUpdateBO(id, sort));
            }
        }
        if (!updates.isEmpty()) {
            itemMapper.updateSorts(params.getProjectId(), updates);
        }
    }

    private AppException orderConflict() {
        return new AppException(ResultCode.OPERATION_FAIL, "项目列表或顺序已变化，请重新加载后再试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProjectItem(ItemCreateParamsBO params) {
        lockProjectCreator(params.getProjectId(), params.getCreatorId(), params.getOrgId());

        TaskTemplate template = taskTemplateMapper.selectById(params.getTaskTemplateId());
        if (template == null || template.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.PARAM_ERROR, "任务模板不存在或已禁用");
        }
        validateTemplateAccess(template, params.getOrgId(), params.getCreatorId());

        List<TaskTemplateNode> templateNodes = taskTemplateNodeMapper.selectList(
                new LambdaQueryWrapper<TaskTemplateNode>()
                        .eq(TaskTemplateNode::getTaskTemplateId, params.getTaskTemplateId())
                        .orderByAsc(TaskTemplateNode::getSort)
                        .orderByAsc(TaskTemplateNode::getParallelSort));

        List<Long> baseTaskIds = templateNodes.stream()
                .map(TaskTemplateNode::getBaseTaskId).filter(id -> id != null).distinct().sorted().toList();
        List<BaseTask> baseTasks = baseTaskIds.isEmpty()
                ? Collections.emptyList()
                : baseTaskMapper.selectList(new LambdaQueryWrapper<BaseTask>()
                        .in(BaseTask::getId, baseTaskIds)
                        .orderByAsc(BaseTask::getId)
                        .last("FOR UPDATE"));
        if (baseTasks.size() != baseTaskIds.size()) {
            throw new AppException(ResultCode.PARAM_ERROR, "任务不存在");
        }
        // 个人模板可跨组织复用，但节点快照只能来自模板所属组织，不能抄入其他组织的任务名。
        if (baseTasks.stream().anyMatch(task -> !Objects.equals(task.getOrgId(), template.getOrgId()))) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权引用该任务");
        }
        Map<Long, BaseTask> baseTaskMap = baseTasks.stream()
                .collect(Collectors.toMap(BaseTask::getId, Function.identity()));

        LocalDateTime now = LocalDateTime.now(clock);
        Item lastItem = itemMapper.selectOne(new LambdaQueryWrapper<Item>()
                .select(Item::getSort)
                .eq(Item::getProjectId, params.getProjectId())
                .orderByDesc(Item::getSort)
                .last("LIMIT 1"));
        long nextSort = lastItem == null ? 1L : lastItem.getSort() + 1L;

        Item item = Item.builder()
                .projectId(params.getProjectId())
                .title(params.getTitle())
                .sort(nextSort)
                .taskTemplateId(params.getTaskTemplateId())
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        itemMapper.insert(item);

        // 项目引用了该任务模板，引用计数 +1
        taskTemplateMapper.update(null,
                new LambdaUpdateWrapper<TaskTemplate>()
                        .eq(TaskTemplate::getId, params.getTaskTemplateId())
                        .setSql("ref_count = ref_count + 1")
                        .set(TaskTemplate::getUpdatedAt, now));

        ItemTaskFlow flow = ItemTaskFlow.builder()
                .itemId(item.getId())
                .projectId(params.getProjectId())
                .taskTemplateId(params.getTaskTemplateId())
                .name(template.getName())
                .description(template.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();
        itemTaskFlowMapper.insert(flow);

        for (TaskTemplateNode templateNode : templateNodes) {
            BaseTask baseTask = baseTaskMap.get(templateNode.getBaseTaskId());
            ItemTaskNode node = ItemTaskNode.builder()
                    .itemTaskFlowId(flow.getId())
                    .itemId(item.getId())
                    .projectId(params.getProjectId())
                    .baseTaskId(templateNode.getBaseTaskId())
                    .name(baseTask != null ? baseTask.getName() : null)
                    .metaSchema(baseTask != null ? baseTask.getMetaSchema() : null)
                    .sort(templateNode.getSort())
                    .parallelSort(templateNode.getParallelSort())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            itemTaskNodeMapper.insert(node);

            TaskInstance instance = TaskInstance.builder()
                    .projectId(params.getProjectId())
                    .itemId(item.getId())
                    .itemTaskNodeId(node.getId())
                    .taskFlowId(flow.getId())
                    .status(TaskInstanceStatus.PENDING)
                    .assigneeId(null)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            taskInstanceMapper.insert(instance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectItemForReview(Long projectId, Long itemId, Long orgId) {
        projectService.lockEnabledProject(new ProjectAccessParamsBO(projectId, orgId));
        softDeleteItem(requireItem(projectId, itemId), orgId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectItemsForReview(Long projectId, Long orgId) {
        projectService.lockEnabledProject(new ProjectAccessParamsBO(projectId, orgId));
        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>().eq(Item::getProjectId, projectId));
        for (Item item : items) {
            softDeleteItem(item, orgId);
        }
    }

    @Override
    public boolean hasProjectItems(Long projectId) {
        return itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getProjectId, projectId)) > 0;
    }

    @Override
    public String requireItemTitle(Long projectId, Long itemId) {
        return requireItem(projectId, itemId).getTitle();
    }

    private void markSubmittedTasks(List<ProjectItemListVO> list) {
        List<Long> itemIds = list.stream().map(ProjectItemListVO::getId).toList();
        List<Object> submittedIds = taskSubmissionMapper.selectObjs(new LambdaQueryWrapper<TaskSubmission>()
                .select(TaskSubmission::getItemId)
                .in(TaskSubmission::getItemId, itemIds)
                .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED));
        if (submittedIds == null || submittedIds.isEmpty()) {
            list.forEach(item -> item.setHasSubmittedTask(false));
            return;
        }
        java.util.Set<Long> submitted = submittedIds.stream()
                .map(id -> ((Number) id).longValue())
                .collect(java.util.stream.Collectors.toSet());
        list.forEach(item -> item.setHasSubmittedTask(submitted.contains(item.getId())));
    }

    @Override
    public boolean hasSubmittedTask(Long itemId) {
        return taskSubmissionMapper.selectCount(new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getItemId, itemId)
                .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED)) > 0;
    }

    private Item requireItem(Long projectId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || !item.getProjectId().equals(projectId)) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }
        return item;
    }

    private void softDeleteItem(Item item, Long orgId) {
        List<Long> instanceIds = taskInstanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                        .select(TaskInstance::getId)
                        .eq(TaskInstance::getItemId, item.getId()))
                .stream()
                .map(TaskInstance::getId)
                .toList();
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = itemMapper.update(null,
                new LambdaUpdateWrapper<Item>()
                        .eq(Item::getId, item.getId())
                        .set(Item::getDeletedAt, now)
                        .set(Item::getUpdatedAt, now));
        if (updated == 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "项目已删除，不可重复操作");
        }

        itemTaskFlowMapper.update(null,
                new LambdaUpdateWrapper<ItemTaskFlow>()
                        .eq(ItemTaskFlow::getItemId, item.getId())
                        .set(ItemTaskFlow::getDeletedAt, now)
                        .set(ItemTaskFlow::getUpdatedAt, now));
        itemTaskNodeMapper.update(null,
                new LambdaUpdateWrapper<ItemTaskNode>()
                        .eq(ItemTaskNode::getItemId, item.getId())
                        .set(ItemTaskNode::getDeletedAt, now)
                        .set(ItemTaskNode::getUpdatedAt, now));
        taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getItemId, item.getId())
                        .set(TaskInstance::getDeletedAt, now)
                        .set(TaskInstance::getUpdatedAt, now));
        taskTemplateMapper.update(null,
                new LambdaUpdateWrapper<TaskTemplate>()
                        .eq(TaskTemplate::getId, item.getTaskTemplateId())
                        .gt(TaskTemplate::getRefCount, 0)
                        .setSql("ref_count = ref_count - 1")
                        .set(TaskTemplate::getUpdatedAt, now));
        if (!instanceIds.isEmpty()) {
            resourceService.abandonBound(ResourceAbandonParamsBO.builder()
                    .orgId(orgId)
                    .bizType("task_submit")
                    .bizIds(instanceIds)
                    .build());
        }
    }


    private void checkProjectCreator(Long projectId, Long userId, Long orgId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getOrgId().equals(orgId)
                || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT);
        }
        if (!project.getCreatorId().equals(userId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION);
        }
    }

    private void lockProjectCreator(Long projectId, Long userId, Long orgId) {
        Long creatorId = projectService.lockEnabledProject(new ProjectAccessParamsBO(projectId, orgId))
                .getCreatorId();
        if (!creatorId.equals(userId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION);
        }
    }

    /**
     * 与可选模板列表规则一致：组织模板须属当前组织；个人模板须为操作人本人创建。
     */
    private void validateTemplateAccess(TaskTemplate template, Long orgId, Long creatorId) {
        if (template.getScope() == TaskTemplateScope.ORGANIZATION) {
            if (!orgId.equals(template.getOrgId())) {
                throw new AppException(ResultCode.PARAM_ERROR, "任务模板不属于当前组织");
            }
            return;
        }
        if (template.getScope() == TaskTemplateScope.PERSONAL) {
            if (!creatorId.equals(template.getCreatorId())) {
                throw new AppException(ResultCode.PARAM_ERROR, "无权使用他人个人模板");
            }
            return;
        }
        throw new AppException(ResultCode.PARAM_ERROR, "任务模板作用域无效");
    }
}
