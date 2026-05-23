package online.longlian.app.service.app.impl.item;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.ItemOperationParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;
import online.longlian.app.service.app.ItemService;
import online.longlian.common.enumeration.ItemStatus;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;
    private final ItemTaskFlowMapper itemTaskFlowMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final BaseTaskMapper baseTaskMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final Clock clock;
    private final ItemQueryBuilder itemQueryBuilder;
    private final ItemAssembler itemAssembler;

    @Override
    public PageResultBO<ProjectItemListVO> listProjectItems(ItemListParamsBO params) {
        Page<Item> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<Item> itemPage = itemMapper.selectPage(page, itemQueryBuilder.buildListQuery(params));
        List<Item> items = itemPage.getRecords();
        if (items.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), itemPage.getTotal());
        }
        return new PageResultBO<>(itemAssembler.assembleList(items), itemPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProjectItem(ItemCreateParamsBO params) {
        TaskTemplate template = taskTemplateMapper.selectById(params.getTaskTemplateId());
        if (template == null || template.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.PARAM_ERROR, "任务模板不存在或已禁用");
        }

        List<TaskTemplateNode> templateNodes = taskTemplateNodeMapper.selectList(
                new LambdaQueryWrapper<TaskTemplateNode>()
                        .eq(TaskTemplateNode::getTaskTemplateId, params.getTaskTemplateId())
                        .isNull(TaskTemplateNode::getDeletedAt)
                        .orderByAsc(TaskTemplateNode::getSort)
                        .orderByAsc(TaskTemplateNode::getParallelSort));

        List<Long> baseTaskIds = templateNodes.stream()
                .map(TaskTemplateNode::getBaseTaskId).distinct().toList();
        Map<Long, BaseTask> baseTaskMap = baseTaskIds.isEmpty()
                ? Collections.emptyMap()
                : baseTaskMapper.selectBatchIds(baseTaskIds).stream()
                        .collect(Collectors.toMap(BaseTask::getId, Function.identity()));

        LocalDateTime now = LocalDateTime.now(clock);

        Item item = Item.builder()
                .projectId(params.getProjectId())
                .title(params.getTitle())
                .taskTemplateId(params.getTaskTemplateId())
                .status(ItemStatus.IN_PROGRESS)
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        itemMapper.insert(item);

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

        for (int i = 0; i < templateNodes.size(); i++) {
            TaskTemplateNode templateNode = templateNodes.get(i);
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

            boolean isFirstNode = i == 0;
            TaskInstance instance = TaskInstance.builder()
                    .projectId(params.getProjectId())
                    .itemId(item.getId())
                    .itemTaskNodeId(node.getId())
                    .taskFlowId(flow.getId())
                    .status(isFirstNode ? TaskInstanceStatus.CLAIMED : TaskInstanceStatus.PENDING)
                    .assigneeId(isFirstNode ? params.getCreatorId() : null)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            taskInstanceMapper.insert(instance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectItem(ItemOperationParamsBO params) {
        Item item = itemMapper.selectById(params.getItemId());
        if (item == null || !item.getProjectId().equals(params.getProjectId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        itemMapper.update(null,
                new LambdaUpdateWrapper<Item>()
                        .eq(Item::getId, params.getItemId())
                        .set(Item::getDeletedAt, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishProjectItem(ItemOperationParamsBO params) {
        Item item = itemMapper.selectById(params.getItemId());
        if (item == null || !item.getProjectId().equals(params.getProjectId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }
        if (item.getStatus() == ItemStatus.PUBLISHED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "项目已公布，不可重复操作");
        }

        itemMapper.update(null,
                new LambdaUpdateWrapper<Item>()
                        .eq(Item::getId, params.getItemId())
                        .set(Item::getStatus, ItemStatus.PUBLISHED)
                        .set(Item::getUpdatedAt, LocalDateTime.now(clock)));
    }
}
