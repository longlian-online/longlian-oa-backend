package online.longlian.app.service.app.impl.itemtaskflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.ItemTaskFlowUpdateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAccessParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAccessResultBO;
import online.longlian.app.pojo.bo.common.ResourceAbandonParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.orgadmin.BaseTaskService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 把一次工作流保存写进数据库。
 * <p>
 * 先锁住任务流、节点和任务实例，交给 {@link ItemTaskFlowEditPlanner} 对照客户端修订号生成计划，
 * 再按保留、删除、新增落库。任一步修订号对不上就整次失败，避免用旧页面覆盖期间的接取或提交。
 */
@Component
@RequiredArgsConstructor
public class ItemTaskFlowEditor {

    private final ProjectService projectService;
    private final BaseTaskService baseTaskService;
    private final ItemMapper itemMapper;
    private final ItemTaskFlowMapper itemTaskFlowMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final BaseTaskMapper baseTaskMapper;
    private final ResourceService resourceService;
    private final Clock clock;

    /**
     * 保存项目工作流。入参形状已由调用方校验，这里负责权限、加锁和落库。
     */
    public void update(ItemTaskFlowUpdateParamsBO params) {
        Item item = itemMapper.selectById(params.getItemId());
        if (item == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }
        // 锁住企划，只允许负责人改这条工作流。
        ProjectAccessResultBO project = projectService.lockEnabledProject(
                new ProjectAccessParamsBO(item.getProjectId(), params.getOrgId()));
        if (!Objects.equals(project.getCreatorId(), params.getOperatorId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅企划负责人可编辑项目工作流");
        }

        // 一起锁住任务流、节点和实例，避免和接取、提交交错读到一半的数据。
        ItemTaskFlow flow = itemTaskFlowMapper.selectOne(new LambdaQueryWrapper<ItemTaskFlow>()
                .eq(ItemTaskFlow::getItemId, item.getId())
                .last("FOR UPDATE"));
        if (flow == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目任务流不存在");
        }
        List<ItemTaskNode> nodes = itemTaskNodeMapper.selectList(new LambdaQueryWrapper<ItemTaskNode>()
                .eq(ItemTaskNode::getItemTaskFlowId, flow.getId())
                .orderByAsc(ItemTaskNode::getId)
                .last("FOR UPDATE"));
        List<TaskInstance> instances = taskInstanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getItemId, item.getId())
                .orderByAsc(TaskInstance::getId)
                .last("FOR UPDATE"));
        // 一个节点应该只有一条任务实例。出现多条说明数据已不一致，拒绝这次保存。
        Map<Long, TaskInstance> instanceByNodeId = instances.stream()
                .collect(Collectors.toMap(TaskInstance::getItemTaskNodeId, Function.identity(), (left, right) -> {
                    throw new AppException(ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
                }));
        // 放弃后会回到待接取并清空接取人，但历史提交还在，这种节点也不能删。
        Map<Long, Integer> submissionCounts = submissionCounts(instances);

        ItemTaskFlowEditPlanner.Plan plan = ItemTaskFlowEditPlanner.plan(
                nodes.stream().map(node -> toCurrent(node, instanceByNodeId, submissionCounts)).toList(),
                params.getNodes().stream().map(this::toDesired).toList(),
                params.getRemoved() == null ? List.of() : params.getRemoved().stream().map(this::toRemoved).toList());

        // 计划只描述变更。任一步乐观锁失败都会抛错，由外层事务整次回滚。
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, BaseTask> createdTasks = loadCreatedTasks(params.getOrgId(), plan.created());
        for (ItemTaskFlowEditPlanner.Kept kept : plan.kept()) {
            updateKept(kept, now);
        }
        for (ItemTaskFlowEditPlanner.Deleted deleted : plan.deleted()) {
            deleteNode(deleted, now);
        }
        abandonDeletedTaskResources(params.getOrgId(), plan.deleted());
        for (ItemTaskFlowEditPlanner.Created created : plan.created()) {
            insertNode(item, flow, created, createdTasks.get(created.baseTaskId()), now);
        }
        itemTaskFlowMapper.update(null, new LambdaUpdateWrapper<ItemTaskFlow>()
                .eq(ItemTaskFlow::getId, flow.getId())
                .set(ItemTaskFlow::getUpdatedAt, now));
    }

    /** 节点表没有修订号，修订号在对应的任务实例上。实例缺失就当成任务流已变。 */
    private ItemTaskFlowEditPlanner.CurrentNode toCurrent(ItemTaskNode node,
                                                          Map<Long, TaskInstance> instanceByNodeId,
                                                          Map<Long, Integer> submissionCounts) {
        TaskInstance instance = instanceByNodeId.get(node.getId());
        if (instance == null || instance.getRevision() == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
        }
        return new ItemTaskFlowEditPlanner.CurrentNode(
                node.getId(),
                node.getBaseTaskId(),
                node.getName(),
                instance.getId(),
                instance.getRevision(),
                instance.getStatus(),
                instance.getAssigneeId(),
                submissionCounts.getOrDefault(instance.getId(), 0));
    }

    /** 没有节点 id 的是新增；已有节点必须带上打开编辑时看到的修订号。 */
    private ItemTaskFlowEditPlanner.DesiredNode toDesired(ItemTaskFlowUpdateParamsBO.Node node) {
        if (node.getBaseTaskId() == null || node.getSort() == null || node.getParallelSort() == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "流程位置无效");
        }
        if (node.getId() != null && node.getRevision() == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "任务修订号不能为空");
        }
        return new ItemTaskFlowEditPlanner.DesiredNode(
                node.getId(), node.getBaseTaskId(), node.getSort(), node.getParallelSort(), node.getName(), node.getRevision());
    }

    /** 删除同样要带修订号，避免删掉页面打开之后才被接取的任务。 */
    private ItemTaskFlowEditPlanner.RemovedNode toRemoved(ItemTaskFlowUpdateParamsBO.Removed node) {
        if (node.getId() == null || node.getRevision() == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "任务修订号不能为空");
        }
        return new ItemTaskFlowEditPlanner.RemovedNode(node.getId(), node.getRevision());
    }

    /** 按任务实例统计提交次数。有过提交的节点不能删，即使当前仍是待接取。 */
    private Map<Long, Integer> submissionCounts(List<TaskInstance> instances) {
        if (instances.isEmpty()) {
            return Map.of();
        }
        List<Long> instanceIds = instances.stream().map(TaskInstance::getId).toList();
        Map<Long, Integer> counts = new HashMap<>();
        for (TaskSubmission submission : taskSubmissionMapper.selectList(new LambdaQueryWrapper<TaskSubmission>()
                .select(TaskSubmission::getTaskInstanceId)
                .in(TaskSubmission::getTaskInstanceId, instanceIds))) {
            counts.merge(submission.getTaskInstanceId(), 1, Integer::sum);
        }
        return counts;
    }

    /** 新节点要抄原子任务的名称和表单快照，先锁住这些原子任务，避免保存过程中被改。 */
    private Map<Long, BaseTask> loadCreatedTasks(Long orgId, List<ItemTaskFlowEditPlanner.Created> created) {
        if (created.isEmpty()) {
            return Map.of();
        }
        List<Long> baseTaskIds = created.stream().map(ItemTaskFlowEditPlanner.Created::baseTaskId).distinct().toList();
        baseTaskService.lockBaseTasks(orgId, baseTaskIds);
        return baseTaskMapper.selectBatchIds(baseTaskIds).stream()
                .collect(Collectors.toMap(BaseTask::getId, Function.identity()));
    }

    /**
     * 保留的节点只改名称和顺序。任务实例用修订号做乐观锁，成功后加一，
     * 让打开编辑时拿到的旧修订号不能再次保存。
     */
    private void updateKept(ItemTaskFlowEditPlanner.Kept kept, LocalDateTime now) {
        itemTaskNodeMapper.update(null, new LambdaUpdateWrapper<ItemTaskNode>()
                .eq(ItemTaskNode::getId, kept.nodeId())
                .set(ItemTaskNode::getSort, kept.sort())
                .set(ItemTaskNode::getParallelSort, kept.parallelSort())
                .set(ItemTaskNode::getName, kept.name())
                .set(ItemTaskNode::getUpdatedAt, now));
        int updated = taskInstanceMapper.update(null, new LambdaUpdateWrapper<TaskInstance>()
                .eq(TaskInstance::getId, kept.instanceId())
                .eq(TaskInstance::getRevision, kept.revision())
                .setSql("revision = revision + 1")
                .set(TaskInstance::getUpdatedAt, now));
        if (updated != 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
        }
    }

    /**
     * 被删节点上仍在使用的任务附件进入保留期，和删除项目同一口径。
     * 没有删除时不查资源；乐观锁失败会先抛错，已做的软删和外层事务一起回滚。
     */
    private void abandonDeletedTaskResources(Long orgId, List<ItemTaskFlowEditPlanner.Deleted> deleted) {
        if (deleted.isEmpty()) {
            return;
        }
        resourceService.abandonBound(ResourceAbandonParamsBO.builder()
                .orgId(orgId)
                .bizType("task_submit")
                .bizIds(deleted.stream().map(ItemTaskFlowEditPlanner.Deleted::instanceId).toList())
                .build());
    }

    /**
     * 先按修订号、待接取、无人接取软删任务实例。条件写在 UPDATE 里，
     * 计划生成后若已被接取，更新行数不是 1，整次保存失败。节点随后软删。
     */
    private void deleteNode(ItemTaskFlowEditPlanner.Deleted deleted, LocalDateTime now) {
        int updated = taskInstanceMapper.update(null, new LambdaUpdateWrapper<TaskInstance>()
                .eq(TaskInstance::getId, deleted.instanceId())
                .eq(TaskInstance::getRevision, deleted.revision())
                .eq(TaskInstance::getStatus, TaskInstanceStatus.PENDING)
                .isNull(TaskInstance::getAssigneeId)
                .set(TaskInstance::getDeletedAt, now)
                .set(TaskInstance::getUpdatedAt, now));
        if (updated != 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
        }
        itemTaskNodeMapper.update(null, new LambdaUpdateWrapper<ItemTaskNode>()
                .eq(ItemTaskNode::getId, deleted.nodeId())
                .set(ItemTaskNode::getDeletedAt, now)
                .set(ItemTaskNode::getUpdatedAt, now));
    }

    /**
     * 新节点和它的任务实例一起插入。实例从待接取开始，修订号用表默认值 0。
     * 名称为空时用原子任务名称，表单定义抄当时的原子任务快照。
     */
    private void insertNode(Item item, ItemTaskFlow flow, ItemTaskFlowEditPlanner.Created created,
                            BaseTask baseTask, LocalDateTime now) {
        if (baseTask == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "任务不存在");
        }
        String name = created.name() == null ? baseTask.getName() : created.name();
        ItemTaskNode node = ItemTaskNode.builder()
                .itemTaskFlowId(flow.getId())
                .itemId(item.getId())
                .projectId(item.getProjectId())
                .baseTaskId(created.baseTaskId())
                .name(name)
                .metaSchema(baseTask.getMetaSchema())
                .sort(created.sort())
                .parallelSort(created.parallelSort())
                .createdAt(now)
                .updatedAt(now)
                .build();
        itemTaskNodeMapper.insert(node);
        taskInstanceMapper.insert(TaskInstance.builder()
                .projectId(item.getProjectId())
                .itemId(item.getId())
                .itemTaskNodeId(node.getId())
                .taskFlowId(flow.getId())
                .status(TaskInstanceStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
