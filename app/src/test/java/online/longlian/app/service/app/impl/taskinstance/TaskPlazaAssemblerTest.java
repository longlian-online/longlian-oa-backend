package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.pojo.bo.app.PlazaItemStageBO;
import online.longlian.app.pojo.bo.app.PlazaProjectSummaryBO;
import online.longlian.app.pojo.bo.app.PlazaTaskRowBO;
import online.longlian.app.pojo.vo.app.PlazaProjectGroupVO;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaskPlazaAssemblerTest {

    private final TaskPlazaAssembler assembler = new TaskPlazaAssembler();

    @Test
    void assembleGroups_emptySummaries_returnsEmpty() {
        List<PlazaProjectGroupVO> result = assembler.assembleGroups(
                List.of(), List.of(), Map.of(), Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void assembleGroups_parallelSlots_distinguishesFilledAndPending() {
        // 阶段1并行双槽：1个已完成 + 1个待接取 → 槽位 1 实 / 1 空
        PlazaTaskRowBO completed = row(11L, 100L, 200L, "翻译", 1, 1, TaskInstanceStatus.COMPLETED);
        PlazaTaskRowBO pending = row(12L, 100L, 200L, "校对", 1, 2, TaskInstanceStatus.PENDING);
        PlazaProjectSummaryBO summary = summary(100L, "企划A", 2L, 1L);
        Map<Long, List<Integer>> itemSorts = Map.of(200L, List.of(1, 2));

        List<PlazaProjectGroupVO> groups = assembler.assembleGroups(
                List.of(summary), List.of(completed, pending), itemSorts, Map.of(100L, "http://cover/100"));

        assertThat(groups).hasSize(1);
        PlazaProjectGroupVO group = groups.getFirst();
        assertThat(group.getProjectId()).isEqualTo(100L);
        assertThat(group.getTitle()).isEqualTo("企划A");
        assertThat(group.getCoverUrl()).isEqualTo("http://cover/100");
        assertThat(group.getPendingCount()).isEqualTo(2);
        assertThat(group.getItemCount()).isEqualTo(1);
        assertThat(group.getTasks()).hasSize(1);
        var task = group.getTasks().getFirst();
        assertThat(task.getInstanceId()).isEqualTo(12L);
        assertThat(task.getNodeName()).isEqualTo("校对");
        assertThat(task.getStageIndex()).isEqualTo(1);
        assertThat(task.getStageTotal()).isEqualTo(2);
        assertThat(task.getParallelTotal()).isEqualTo(2);
        assertThat(task.getFilledSlots()).isEqualTo(1);
        assertThat(task.getPendingSlots()).isEqualTo(1);
    }

    @Test
    void assembleGroups_multipleItems_ordersByItemThenStage() {
        // 同企划两个条目：行按 条目→阶段 序输入，输出保持该顺序
        PlazaTaskRowBO itemAStage1 = row(11L, 100L, 200L, "翻译", 1, 1, TaskInstanceStatus.PENDING);
        PlazaTaskRowBO itemAStage2 = row(12L, 100L, 200L, "嵌字", 2, 1, TaskInstanceStatus.PENDING);
        PlazaTaskRowBO itemBStage1 = row(13L, 100L, 201L, "翻译", 1, 1, TaskInstanceStatus.PENDING);
        PlazaProjectSummaryBO summary = summary(100L, "企划A", 3L, 2L);
        Map<Long, List<Integer>> itemSorts = Map.of(
                200L, List.of(1, 2),
                201L, List.of(1));

        List<PlazaProjectGroupVO> groups = assembler.assembleGroups(
                List.of(summary), List.of(itemAStage1, itemAStage2, itemBStage1), itemSorts, Map.of());

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getTasks()).hasSize(3);
        var group = groups.getFirst();
        assertThat(group.getItemCount()).isEqualTo(2);
        assertThat(group.getTasks().get(0).getItemId()).isEqualTo(200L);
        assertThat(group.getTasks().get(0).getStageIndex()).isEqualTo(1);
        assertThat(group.getTasks().get(1).getStageIndex()).isEqualTo(2);
        assertThat(group.getTasks().get(2).getItemId()).isEqualTo(201L);
        assertThat(group.getTasks().get(2).getStageTotal()).isEqualTo(1);
    }

    @Test
    void assembleGroups_multipleProjects_splitsRowsByProject() {
        PlazaTaskRowBO projectATask = row(11L, 100L, 200L, "翻译", 1, 1, TaskInstanceStatus.PENDING);
        PlazaTaskRowBO projectBTask = row(21L, 101L, 210L, "翻译", 1, 1, TaskInstanceStatus.PENDING);
        Map<Long, List<Integer>> itemSorts = Map.of(200L, List.of(1), 210L, List.of(1));

        List<PlazaProjectGroupVO> groups = assembler.assembleGroups(
                List.of(summary(100L, "企划A", 1L, 1L), summary(101L, "企划B", 1L, 1L)),
                List.of(projectATask, projectBTask), itemSorts, Map.of());

        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).getProjectId()).isEqualTo(100L);
        assertThat(groups.get(0).getTasks()).hasSize(1);
        assertThat(groups.get(1).getProjectId()).isEqualTo(101L);
        assertThat(groups.get(1).getTasks()).hasSize(1);
    }

    private PlazaProjectSummaryBO summary(Long projectId, String title, Long pendingCount, Long itemCount) {
        PlazaProjectSummaryBO summary = new PlazaProjectSummaryBO();
        summary.setProjectId(projectId);
        summary.setTitle(title);
        summary.setTypeName("漫画");
        summary.setPendingCount(pendingCount);
        summary.setItemCount(itemCount);
        summary.setOldestCreatedAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        summary.setLatestCreatedAt(LocalDateTime.of(2026, 10, 2, 0, 0));
        return summary;
    }

    private PlazaTaskRowBO row(Long instanceId, Long projectId, Long itemId, String nodeName,
                               int sort, int parallelSort, TaskInstanceStatus status) {
        PlazaTaskRowBO row = new PlazaTaskRowBO();
        row.setInstanceId(instanceId);
        row.setProjectId(projectId);
        row.setItemId(itemId);
        row.setItemTitle("第04话");
        row.setNodeName(nodeName);
        row.setSort(sort);
        row.setParallelSort(parallelSort);
        row.setStatus(status);
        row.setCreatedAt(LocalDateTime.of(2026, 10, 1, 12, 0));
        return row;
    }
}
