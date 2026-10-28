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
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.orgadmin.BaseTaskService;
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
    private final Clock clock;

    public void update(ItemTaskFlowUpdateParamsBO params) {
        Item item = itemMapper.selectById(params.getItemId());
        if (item == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }
        ProjectAccessResultBO project = projectService.lockEnabledProject(
                new ProjectAccessParamsBO(item.getProjectId(), params.getOrgId()));
        if (!Objects.equals(project.getCreatorId(), params.getOperatorId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅企划负责人可编辑项目工作流");
        }

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
        Map<Long, TaskInstance> instanceByNodeId = instances.stream()
                .collect(Collectors.toMap(TaskInstance::getItemTaskNodeId, Function.identity(), (left, right) -> {
                    throw new AppException(ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
                }));
        Map<Long, Integer> submissionCounts = submissionCounts(instances);

        ItemTaskFlowEditPlanner.Plan plan = ItemTaskFlowEditPlanner.plan(
                nodes.stream().map(node -> toCurrent(node, instanceByNodeId, submissionCounts)).toList(),
                params.getNodes().stream().map(this::toDesired).toList(),
                params.getRemoved() == null ? List.of() : params.getRemoved().stream().map(this::toRemoved).toList());

        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, BaseTask> createdTasks = loadCreatedTasks(params.getOrgId(), plan.created());
        for (ItemTaskFlowEditPlanner.Kept kept : plan.kept()) {
            updateKept(kept, now);
        }
        for (ItemTaskFlowEditPlanner.Deleted deleted : plan.deleted()) {
            deleteNode(deleted, now);
        }
        for (ItemTaskFlowEditPlanner.Created created : plan.created()) {
            insertNode(item, flow, created, createdTasks.get(created.baseTaskId()), now);
        }
        itemTaskFlowMapper.update(null, new LambdaUpdateWrapper<ItemTaskFlow>()
                .eq(ItemTaskFlow::getId, flow.getId())
                .set(ItemTaskFlow::getUpdatedAt, now));
    }

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

    private ItemTaskFlowEditPlanner.RemovedNode toRemoved(ItemTaskFlowUpdateParamsBO.Removed node) {
        if (node.getId() == null || node.getRevision() == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "任务修订号不能为空");
        }
        return new ItemTaskFlowEditPlanner.RemovedNode(node.getId(), node.getRevision());
    }

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

    private Map<Long, BaseTask> loadCreatedTasks(Long orgId, List<ItemTaskFlowEditPlanner.Created> created) {
        if (created.isEmpty()) {
            return Map.of();
        }
        List<Long> baseTaskIds = created.stream().map(ItemTaskFlowEditPlanner.Created::baseTaskId).distinct().toList();
        baseTaskService.lockBaseTasks(orgId, baseTaskIds);
        return baseTaskMapper.selectBatchIds(baseTaskIds).stream()
                .collect(Collectors.toMap(BaseTask::getId, Function.identity()));
    }

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
