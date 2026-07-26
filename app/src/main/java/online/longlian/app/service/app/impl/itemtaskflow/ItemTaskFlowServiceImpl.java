package online.longlian.app.service.app.impl.itemtaskflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.vo.app.ItemTaskFlowVO;
import online.longlian.app.service.app.ItemTaskFlowService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemTaskFlowServiceImpl implements ItemTaskFlowService {

    private final ItemMapper itemMapper;
    private final ItemTaskFlowMapper itemTaskFlowMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final ItemTaskFlowAssembler itemTaskFlowAssembler;
    private final ProjectMapper projectMapper;

    @Override
    public ItemTaskFlowVO getItemTaskFlow(Long itemId, Long orgId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }

        Project project = projectMapper.selectById(item.getProjectId());
        if (project == null || !project.getOrgId().equals(orgId)) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }

        ItemTaskFlow flow = itemTaskFlowMapper.selectOne(
                new LambdaQueryWrapper<ItemTaskFlow>()
                        .eq(ItemTaskFlow::getItemId, itemId)
                        .last("LIMIT 1"));
        if (flow == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目任务流不存在");
        }

        List<ItemTaskNode> nodes = itemTaskNodeMapper.selectList(
                new LambdaQueryWrapper<ItemTaskNode>()
                        .eq(ItemTaskNode::getItemTaskFlowId, flow.getId())
                        .orderByAsc(ItemTaskNode::getSort)
                        .orderByAsc(ItemTaskNode::getParallelSort));

        List<Long> nodeIds = nodes.stream().map(ItemTaskNode::getId).toList();
        List<TaskInstance> instances = nodeIds.isEmpty()
                ? Collections.emptyList()
                : taskInstanceMapper.selectList(
                        new LambdaQueryWrapper<TaskInstance>()
                                .in(TaskInstance::getItemTaskNodeId, nodeIds));

        return ItemTaskFlowVO.builder()
                .name(flow.getName())
                .description(flow.getDescription())
                .nodes(itemTaskFlowAssembler.assembleNodes(nodes, instances))
                .build();
    }
}
