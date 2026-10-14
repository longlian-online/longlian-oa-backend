package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * 阶段前置判定（共享口径，与前端 WorkflowInstance {@code isNodeUnlocked} 一致）：
 * 同一项目中 sort 严格小于当前节点的全部节点均已完成，则当前节点已解锁。
 * <p>
 * 目前仅供「我的待办」列表分组使用，未接入 claim / submit 门禁。
 */
public final class StageUnlockPolicy {

    private StageUnlockPolicy() {
    }

    public static boolean isUnlocked(int sort, Collection<ItemStageNodeStatusBO> itemNodes) {
        return itemNodes.stream()
                .filter(node -> node.getSort() < sort)
                .allMatch(node -> Boolean.TRUE.equals(node.getCompleted()));
    }

    /** 最近的未完成前置节点：sort 最大者，同 sort 取 parallelSort、id 最小者 */
    public static Optional<ItemStageNodeStatusBO> nearestIncompletePredecessor(
            int sort, Collection<ItemStageNodeStatusBO> itemNodes) {
        return itemNodes.stream()
                .filter(node -> node.getSort() < sort)
                .filter(node -> !Boolean.TRUE.equals(node.getCompleted()))
                .max(Comparator.comparing(ItemStageNodeStatusBO::getSort)
                        .thenComparing(ItemStageNodeStatusBO::getParallelSort,
                                Comparator.nullsFirst(Comparator.reverseOrder()))
                        .thenComparing(ItemStageNodeStatusBO::getNodeId, Comparator.reverseOrder()));
    }
}
