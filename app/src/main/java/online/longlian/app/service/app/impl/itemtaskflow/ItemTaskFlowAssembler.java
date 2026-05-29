package online.longlian.app.service.app.impl.itemtaskflow;

import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.vo.app.ItemTaskNodeVO;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItemTaskFlowAssembler {

    private final BaseTaskMapper baseTaskMapper;
    private final ResourceService resourceService;

    public List<ItemTaskNodeVO> assembleNodes(List<ItemTaskNode> nodes, List<TaskInstance> instances) {
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, TaskInstance> instanceMap = instances.stream()
                .collect(Collectors.toMap(TaskInstance::getItemTaskNodeId, Function.identity(), (a, b) -> a));

        List<Long> baseTaskIds = nodes.stream().map(ItemTaskNode::getBaseTaskId).distinct().toList();
        Map<Long, BaseTask> baseTaskMap = baseTaskMapper.selectBatchIds(baseTaskIds).stream()
                .collect(Collectors.toMap(BaseTask::getId, Function.identity()));

        List<Long> iconFileIds = baseTaskMap.values().stream()
                .map(BaseTask::getIconFileId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> iconUrlMap = iconFileIds.stream()
                .collect(Collectors.toMap(Function.identity(), resourceService::getResourceReadUrl));

        return nodes.stream()
                .map(node -> {
                    BaseTask baseTask = baseTaskMap.get(node.getBaseTaskId());
                    String iconUrl = null;
                    if (baseTask != null && baseTask.getIconFileId() != null) {
                        iconUrl = iconUrlMap.get(baseTask.getIconFileId());
                    }

                    ItemTaskNodeVO.ItemTaskNodeVOBuilder builder = ItemTaskNodeVO.builder()
                            .id(node.getId())
                            .baseTaskId(node.getBaseTaskId())
                            .name(node.getName())
                            .baseTaskIconUrl(iconUrl)
                            .metaSchema(node.getMetaSchema())
                            .sort(node.getSort())
                            .parallelSort(node.getParallelSort());

                    TaskInstance instance = instanceMap.get(node.getId());
                    if (instance != null) {
                        builder.taskInstanceId(instance.getId())
                                .taskStatus(instance.getStatus());
                    }
                    return builder.build();
                })
                .toList();
    }
}
