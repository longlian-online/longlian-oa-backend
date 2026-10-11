package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.pojo.bo.app.PlazaItemStageBO;
import online.longlian.app.pojo.bo.app.PlazaProjectSummaryBO;
import online.longlian.app.pojo.bo.app.PlazaTaskRowBO;
import online.longlian.app.pojo.vo.app.PlazaProjectGroupVO;
import online.longlian.app.pojo.vo.app.PlazaTaskVO;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
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
     * @param rows                上述企划当前阶段组内的全部任务实例行（含已占用槽位）
     * @param itemSorts           条目ID → 全部阶段 sort 值（升序去重）
     * @param coverUrlByProjectId 企划ID → 封面图URL
     */
    public List<PlazaProjectGroupVO> assembleGroups(List<PlazaProjectSummaryBO> summaries,
                                                    List<PlazaTaskRowBO> rows,
                                                    Map<Long, List<Integer>> itemSorts,
                                                    Map<Long, String> coverUrlByProjectId) {
        if (summaries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<PlazaTaskRowBO>> rowsByProject = rows.stream()
                .collect(Collectors.groupingBy(PlazaTaskRowBO::getProjectId,
                        LinkedHashMap::new, Collectors.toList()));

        return summaries.stream()
                .map(summary -> assembleGroup(summary,
                        rowsByProject.getOrDefault(summary.getProjectId(), Collections.emptyList()),
                        itemSorts, coverUrlByProjectId))
                .toList();
    }

    private PlazaProjectGroupVO assembleGroup(PlazaProjectSummaryBO summary,
                                              List<PlazaTaskRowBO> projectRows,
                                              Map<Long, List<Integer>> itemSorts,
                                              Map<Long, String> coverUrlByProjectId) {
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
                tasks.add(PlazaTaskVO.builder()
                        .instanceId(row.getInstanceId())
                        .itemId(row.getItemId())
                        .itemTitle(row.getItemTitle())
                        .nodeName(row.getNodeName())
                        .stageIndex(sorts.indexOf(row.getSort()) + 1)
                        .stageTotal(sorts.size())
                        .parallelTotal(parallelTotal)
                        .filledSlots(filledSlots)
                        .pendingSlots(pendingSlots)
                        .createdAt(row.getCreatedAt())
                        .build());
            }
        }

        return PlazaProjectGroupVO.builder()
                .projectId(summary.getProjectId())
                .title(summary.getTitle())
                .typeName(summary.getTypeName())
                .coverUrl(coverUrlByProjectId.get(summary.getProjectId()))
                .pendingCount(summary.getPendingCount().intValue())
                .itemCount(summary.getItemCount().intValue())
                .tasks(tasks)
                .build();
    }

    private static String slotKey(PlazaTaskRowBO row) {
        return row.getItemId() + ":" + row.getSort();
    }
}
