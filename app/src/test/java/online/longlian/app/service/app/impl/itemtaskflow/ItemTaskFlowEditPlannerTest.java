package online.longlian.app.service.app.impl.itemtaskflow;

import online.longlian.app.common.exception.AppException;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemTaskFlowEditPlannerTest {

    @Test
    void plan_reordersAndCreatesWhileKeepingRevision() {
        ItemTaskFlowEditPlanner.Plan plan = ItemTaskFlowEditPlanner.plan(
                List.of(current(1L, 10L, "翻译", 100L, 3L, TaskInstanceStatus.COMPLETED, 8L, 1)),
                List.of(
                        desired(null, 20L, 1, 1, "校对", null),
                        desired(1L, 10L, 5, 2, "  ", 3L)),
                List.of());

        assertThat(plan.created()).containsExactly(
                new ItemTaskFlowEditPlanner.Created(20L, 1, 1, "校对"));
        assertThat(plan.kept()).containsExactly(
                new ItemTaskFlowEditPlanner.Kept(1L, 100L, 3L, 2, 1, "翻译"));
        assertThat(plan.deleted()).isEmpty();
    }

    @Test
    void plan_deletesUntouchedPendingNode() {
        ItemTaskFlowEditPlanner.Plan plan = ItemTaskFlowEditPlanner.plan(
                List.of(
                        current(1L, 10L, "翻译", 100L, 0L, TaskInstanceStatus.PENDING, null, 0),
                        current(2L, 20L, "校对", 200L, 0L, TaskInstanceStatus.PENDING, null, 0)),
                List.of(desired(2L, 20L, 4, 1, null, 0L)),
                List.of(new ItemTaskFlowEditPlanner.RemovedNode(1L, 0L)));

        assertThat(plan.deleted()).containsExactly(new ItemTaskFlowEditPlanner.Deleted(1L, 100L, 0L));
        assertThat(plan.kept()).extracting(ItemTaskFlowEditPlanner.Kept::sort).containsExactly(1);
    }

    @Test
    void plan_rejectsDeletingClaimedOrSubmittedNode() {
        assertThatThrownBy(() -> ItemTaskFlowEditPlanner.plan(
                List.of(current(1L, 10L, "翻译", 100L, 1L, TaskInstanceStatus.CLAIMED, 8L, 0),
                        current(2L, 20L, "校对", 200L, 0L, TaskInstanceStatus.PENDING, null, 0)),
                List.of(desired(2L, 20L, 1, 1, null, 0L)),
                List.of(new ItemTaskFlowEditPlanner.RemovedNode(1L, 1L))))
                .isInstanceOf(AppException.class)
                .hasMessage(ItemTaskFlowEditPlanner.LOCKED_DELETE);

        assertThatThrownBy(() -> ItemTaskFlowEditPlanner.plan(
                List.of(current(1L, 10L, "翻译", 100L, 2L, TaskInstanceStatus.PENDING, null, 1),
                        current(2L, 20L, "校对", 200L, 0L, TaskInstanceStatus.PENDING, null, 0)),
                List.of(desired(2L, 20L, 1, 1, null, 0L)),
                List.of(new ItemTaskFlowEditPlanner.RemovedNode(1L, 2L))))
                .isInstanceOf(AppException.class)
                .hasMessage(ItemTaskFlowEditPlanner.LOCKED_DELETE);
    }

    @Test
    void plan_rejectsStaleRevisionChangedBaseTaskAndMissingNode() {
        List<ItemTaskFlowEditPlanner.CurrentNode> current = List.of(
                current(1L, 10L, "翻译", 100L, 4L, TaskInstanceStatus.PENDING, null, 0));

        assertThatThrownBy(() -> ItemTaskFlowEditPlanner.plan(
                current, List.of(desired(1L, 10L, 1, 1, null, 3L)), List.of()))
                .isInstanceOf(AppException.class)
                .hasMessage(ItemTaskFlowEditPlanner.STALE);
        assertThatThrownBy(() -> ItemTaskFlowEditPlanner.plan(
                current, List.of(desired(1L, 99L, 1, 1, null, 4L)), List.of()))
                .isInstanceOf(AppException.class)
                .hasMessage(ItemTaskFlowEditPlanner.BASE_TASK_CHANGED);
        assertThatThrownBy(() -> ItemTaskFlowEditPlanner.plan(
                current, List.of(desired(null, 20L, 1, 1, null, null)), List.of()))
                .isInstanceOf(AppException.class)
                .hasMessage(ItemTaskFlowEditPlanner.STALE);
    }

    private static ItemTaskFlowEditPlanner.CurrentNode current(long id, long baseTaskId, String name,
                                                               long instanceId, long revision,
                                                               TaskInstanceStatus status, Long assigneeId,
                                                               int submissionCount) {
        return new ItemTaskFlowEditPlanner.CurrentNode(
                id, baseTaskId, name, instanceId, revision, status, assigneeId, submissionCount);
    }

    private static ItemTaskFlowEditPlanner.DesiredNode desired(Long id, long baseTaskId, int sort,
                                                               int parallelSort, String name, Long revision) {
        return new ItemTaskFlowEditPlanner.DesiredNode(id, baseTaskId, sort, parallelSort, name, revision);
    }
}
