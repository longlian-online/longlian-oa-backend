package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StageUnlockPolicyTest {

    private static ItemStageNodeStatusBO node(long id, String name, int sort, int parallel, boolean completed) {
        return new ItemStageNodeStatusBO(1L, id, name, sort, parallel, completed);
    }

    @Test
    void firstStage_isUnlocked() {
        assertThat(StageUnlockPolicy.isUnlocked(1, List.of(node(1, "线稿", 1, 1, false)))).isTrue();
    }

    @Test
    void allPredecessorsCompleted_isUnlocked() {
        List<ItemStageNodeStatusBO> nodes = List.of(
                node(1, "线稿", 1, 1, true), node(2, "上色", 2, 1, true), node(3, "嵌字", 3, 1, false));
        assertThat(StageUnlockPolicy.isUnlocked(3, nodes)).isTrue();
        assertThat(StageUnlockPolicy.nearestIncompletePredecessor(3, nodes)).isEmpty();
    }

    @Test
    void sameSortSibling_doesNotBlock() {
        List<ItemStageNodeStatusBO> nodes = List.of(
                node(1, "线稿", 1, 1, true), node(2, "上色A", 2, 1, false), node(3, "上色B", 2, 2, false));
        assertThat(StageUnlockPolicy.isUnlocked(2, nodes)).isTrue();
    }

    @Test
    void incompletePredecessor_locks_andNearestHasMaxSort() {
        List<ItemStageNodeStatusBO> nodes = List.of(
                node(1, "线稿", 1, 1, false), node(2, "上色", 2, 1, false),
                node(5, "校对B", 3, 2, false), node(4, "校对A", 3, 1, false), node(6, "嵌字", 4, 1, false));
        assertThat(StageUnlockPolicy.isUnlocked(4, nodes)).isFalse();
        assertThat(StageUnlockPolicy.nearestIncompletePredecessor(4, nodes))
                .get().extracting(ItemStageNodeStatusBO::getNodeName).isEqualTo("校对A");
    }

    @Test
    void nearestSkipsCompletedLaterPredecessor() {
        List<ItemStageNodeStatusBO> nodes = List.of(
                node(1, "线稿", 1, 1, false), node(2, "上色", 2, 1, true), node(3, "嵌字", 3, 1, false));
        assertThat(StageUnlockPolicy.nearestIncompletePredecessor(3, nodes))
                .get().extracting(ItemStageNodeStatusBO::getNodeName).isEqualTo("线稿");
    }
}
