package online.longlian.app.service.app.impl.taskinstance;

import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadParamsBO;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadResultBO;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadRowBO;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.service.app.ClaimedTaskLoadService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimedTaskLoadServiceImpl implements ClaimedTaskLoadService {

    private final TaskInstanceMapper taskInstanceMapper;

    @Override
    public List<ClaimedTaskLoadResultBO> listClaimedTasks(ClaimedTaskLoadParamsBO params) {
        if (params.getAssigneeIds() == null || params.getAssigneeIds().isEmpty()) {
            return List.of();
        }
        List<ClaimedTaskLoadRowBO> rows = taskInstanceMapper.selectClaimedTasksByAssignees(
                params.getOrgId(), params.getAssigneeIds());
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = rows.stream().map(ClaimedTaskLoadRowBO::getItemId).distinct().toList();
        Map<Long, List<ItemStageNodeStatusBO>> nodesByItem = taskInstanceMapper.selectItemStageNodeStatuses(itemIds)
                .stream().collect(Collectors.groupingBy(ItemStageNodeStatusBO::getItemId));
        return rows.stream()
                .map(row -> toResult(row, nodesByItem.getOrDefault(row.getItemId(), Collections.emptyList())))
                .toList();
    }

    private ClaimedTaskLoadResultBO toResult(ClaimedTaskLoadRowBO row, List<ItemStageNodeStatusBO> itemNodes) {
        Optional<ItemStageNodeStatusBO> waiting = StageUnlockPolicy.nearestIncompletePredecessor(
                row.getStageSort(), itemNodes);
        SlotLabels.Position self = SlotLabels.of(row.getNodeId(), itemNodes);
        SlotLabels.Position blocked = waiting
                .map(node -> SlotLabels.of(node.getNodeId(), itemNodes))
                .orElse(null);
        return ClaimedTaskLoadResultBO.builder()
                .assigneeId(row.getAssigneeId())
                .instanceId(row.getInstanceId())
                .itemId(row.getItemId())
                .projectId(row.getProjectId())
                .itemTitle(row.getItemTitle())
                .projectName(row.getProjectName())
                .nodeName(row.getNodeName())
                .slotIndex(self.slotIndex())
                .slotTotal(self.slotTotal())
                .serialStep(self.serialStep())
                .waitingStageName(waiting.map(ItemStageNodeStatusBO::getNodeName).orElse(null))
                .waitingSlotIndex(blocked == null ? null : blocked.slotIndex())
                .waitingSlotTotal(blocked == null ? null : blocked.slotTotal())
                .waitingSerialStep(blocked == null ? null : blocked.serialStep())
                .build();
    }
}
