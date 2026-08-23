package online.longlian.app.service.app.impl.item;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;
import online.longlian.app.pojo.vo.app.ProjectItemNodeVO;
import online.longlian.common.enumeration.ItemNodeState;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItemAssembler {

    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    public List<ProjectItemListVO> assembleList(List<Item> items) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<Long, List<ItemTaskNode>> nodesByItemId = itemTaskNodeMapper.selectList(
                        new LambdaQueryWrapper<ItemTaskNode>()
                                .in(ItemTaskNode::getItemId, itemIds)
                                .orderByAsc(ItemTaskNode::getSort)
                                .orderByAsc(ItemTaskNode::getParallelSort))
                .stream()
                .collect(Collectors.groupingBy(ItemTaskNode::getItemId));
        List<Long> nodeIds = nodesByItemId.values().stream()
                .flatMap(List::stream)
                .map(ItemTaskNode::getId)
                .toList();
        Map<Long, List<TaskInstance>> instancesByNodeId;
        if (nodeIds.isEmpty()) {
            instancesByNodeId = Collections.emptyMap();
        } else {
            instancesByNodeId = taskInstanceMapper.selectList(
                            new LambdaQueryWrapper<TaskInstance>()
                                    .in(TaskInstance::getItemTaskNodeId, nodeIds)
                                    )
                    .stream()
                    .collect(Collectors.groupingBy(TaskInstance::getItemTaskNodeId));
        }

        return items.stream()
                .map(item -> assembleItem(item,
                        nodesByItemId.getOrDefault(item.getId(), Collections.emptyList()),
                        instancesByNodeId))
                .toList();
    }

    ProjectItemListVO assembleItem(Item item, List<ItemTaskNode> nodes,
                                    Map<Long, List<TaskInstance>> instancesByNodeId) {
        int totalNodes = nodes.size();
        long completedCount = nodes.stream()
                .flatMap(n -> instancesByNodeId.getOrDefault(n.getId(), Collections.emptyList()).stream())
                .filter(i -> i.getStatus() == TaskInstanceStatus.COMPLETED)
                .count();

        Map<Integer, Long> sortCountMap = nodes.stream()
                .collect(Collectors.groupingBy(ItemTaskNode::getSort, Collectors.counting()));

        return ProjectItemListVO.builder()
                .id(item.getId())
                .title(item.getTitle())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .progressPercent(totalNodes > 0 ? (int) (completedCount * 100 / totalNodes) : 0)
                .currentNodeName(resolveCurrentNodeName(nodes, instancesByNodeId))
                .nodes(nodes.stream()
                        .map(node -> toNodeVO(node, instancesByNodeId, sortCountMap))
                        .toList())
                .build();
    }

    String resolveCurrentNodeName(List<ItemTaskNode> nodes,
                                   Map<Long, List<TaskInstance>> instancesByNodeId) {
        for (ItemTaskNode node : nodes) {
            List<TaskInstance> instances = instancesByNodeId.getOrDefault(node.getId(), Collections.emptyList());
            if (instances.stream().anyMatch(i ->
                    i.getStatus() == TaskInstanceStatus.PENDING || i.getStatus() == TaskInstanceStatus.CLAIMED)) {
                return node.getName();
            }
        }
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1).getName();
    }

    ProjectItemNodeVO toNodeVO(ItemTaskNode node,
                                Map<Long, List<TaskInstance>> instancesByNodeId,
                                Map<Integer, Long> sortCountMap) {
        List<TaskInstance> instances = instancesByNodeId.getOrDefault(node.getId(), Collections.emptyList());

        return ProjectItemNodeVO.builder()
                .name(node.getName())
                .sort(node.getSort())
                .parallelSort(node.getParallelSort())
                .state(computeNodeState(instances))
                .parallelCount(sortCountMap.getOrDefault(node.getSort(), 1L).intValue())
                .build();
    }

    ItemNodeState computeNodeState(List<TaskInstance> instances) {
        if (instances.isEmpty()) {
            return ItemNodeState.LOCKED;
        }
        boolean allCompleted = instances.stream().allMatch(i -> i.getStatus() == TaskInstanceStatus.COMPLETED);
        if (allCompleted) {
            return ItemNodeState.COMPLETED;
        }
        return ItemNodeState.IN_PROGRESS;
    }
}
