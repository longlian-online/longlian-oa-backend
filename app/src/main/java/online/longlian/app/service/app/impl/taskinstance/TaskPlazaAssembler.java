package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.PlazaProjectSummaryBO;
import online.longlian.app.pojo.bo.app.PlazaTaskRowBO;
import online.longlian.app.pojo.vo.app.PlazaProjectGroupVO;
import online.longlian.app.pojo.vo.app.PlazaTaskVO;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务广场装配：将企划聚合行与阶段内任务实例行装配为按企划分组的 VO。
 */
@Component
public class TaskPlazaAssembler {

    /**
     * @param summaries           企划聚合行（已分页排序）
     * @param rows                上述企划匹配范围内的任务实例行（含已占用槽位）
     * @param itemSorts           条目ID → 全部阶段 sort 值（升序去重）
     * @param coverUrlByProjectId 企划ID → 封面图URL
     * @param thumbnailUrlByProjectId 企划ID → 缩略图URL
     * @param nodesByItem         条目ID → 全部任务节点。当前页的实例行不一定覆盖更早的同名阶段
     */
    public List<PlazaProjectGroupVO> assembleGroups(List<PlazaProjectSummaryBO> summaries,
                                                    List<PlazaTaskRowBO> rows,
                                                    Map<Long, List<Integer>> itemSorts,
                                                    Map<Long, String> coverUrlByProjectId,
                                                    Map<Long, String> thumbnailUrlByProjectId,
                                                    Map<Long, List<ItemStageNodeStatusBO>> nodesByItem) {
        if (summaries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<PlazaTaskRowBO>> rowsByProject = rows.stream()
                .collect(Collectors.groupingBy(PlazaTaskRowBO::getProjectId,
                        LinkedHashMap::new, Collectors.toList()));

        return summaries.stream()
                .map(summary -> assembleGroup(summary,
                        rowsByProject.getOrDefault(summary.getProjectId(), Collections.emptyList()),
                        itemSorts, coverUrlByProjectId, thumbnailUrlByProjectId, nodesByItem))
                .toList();
    }

    private PlazaProjectGroupVO assembleGroup(PlazaProjectSummaryBO summary,
                                              List<PlazaTaskRowBO> projectRows,
                                              Map<Long, List<Integer>> itemSorts,
                                              Map<Long, String> coverUrlByProjectId,
                                              Map<Long, String> thumbnailUrlByProjectId,
                                              Map<Long, List<ItemStageNodeStatusBO>> nodesByItem) {
        // (条目, 阶段) 分组保持行序；SQL 已按 条目→阶段→并行组→创建时间 排序
        Map<String, List<PlazaTaskRowBO>> slotGroups = projectRows.stream()
                .collect(Collectors.groupingBy(TaskPlazaAssembler::slotKey,
                        LinkedHashMap::new, Collectors.toList()));

        List<PlazaTaskVO> tasks = new ArrayList<>();
        for (List<PlazaTaskRowBO> groupRows : slotGroups.values()) {
            int parallelTotal = groupRows.size();
            int pendingSlots = (int) groupRows.stream()
                    .filter(row -> row.getStatus() == TaskInstanceStatus.PENDING)
                    .count();
            int filledSlots = parallelTotal - pendingSlots;

            for (PlazaTaskRowBO row : groupRows) {
                if (row.getStatus() != TaskInstanceStatus.PENDING) {
                    continue;
                }
                List<Integer> sorts = itemSorts.get(row.getItemId());
                SlotLabels.Position position = slotPosition(row, groupRows,
                        nodesByItem.getOrDefault(row.getItemId(), List.of()));
                tasks.add(PlazaTaskVO.builder()
                        .instanceId(row.getInstanceId())
                        .itemId(row.getItemId())
                        .itemTitle(row.getItemTitle())
                        .nodeName(row.getNodeName())
                        .isCurrentStage(row.getIsCurrentStage())
                        .stageIndex(sorts.indexOf(row.getSort()) + 1)
                        .stageTotal(sorts.size())
                        .parallelTotal(parallelTotal)
                        .filledSlots(filledSlots)
                        .pendingSlots(pendingSlots)
                        .slotIndex(position.slotIndex())
                        .slotTotal(position.slotTotal())
                        .serialStep(position.serialStep())
                        .createdAt(row.getCreatedAt())
                        .build());
            }
        }

        return PlazaProjectGroupVO.builder()
                .projectId(summary.getProjectId())
                .title(summary.getTitle())
                .typeName(summary.getTypeName())
                .coverUrl(coverUrlByProjectId.get(summary.getProjectId()))
                .thumbnailUrl(thumbnailUrlByProjectId.get(summary.getProjectId()))
                .pendingCount(summary.getPendingCount().intValue())
                .itemCount(summary.getItemCount().intValue())
                .tasks(tasks)
                .build();
    }

    /**
     * 节点列表能对上时用全流程算位次；对不上时退回本阶段实例顺序，避免缺节点时把并行槽都标成 1。
     */
    private static SlotLabels.Position slotPosition(PlazaTaskRowBO row,
                                                    List<PlazaTaskRowBO> groupRows,
                                                    List<ItemStageNodeStatusBO> itemNodes) {
        boolean known = row.getNodeId() != null && itemNodes.stream()
                .anyMatch(node -> row.getNodeId().equals(node.getNodeId()));
        if (known) {
            return SlotLabels.of(row.getNodeId(), itemNodes);
        }
        List<PlazaTaskRowBO> ordered = groupRows.stream()
                .sorted(Comparator
                        .comparing(PlazaTaskRowBO::getParallelSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(PlazaTaskRowBO::getInstanceId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        int slotIndex = 1;
        for (int i = 0; i < ordered.size(); i++) {
            if (row.getInstanceId() != null && row.getInstanceId().equals(ordered.get(i).getInstanceId())) {
                slotIndex = i + 1;
                break;
            }
        }
        return new SlotLabels.Position(slotIndex, Math.max(ordered.size(), 1), null);
    }

    private static String slotKey(PlazaTaskRowBO row) {
        return row.getItemId() + ":" + row.getSort();
    }
}
