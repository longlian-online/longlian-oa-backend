package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 槽位只描述结构，不拼展示文案。
 * <p>
 * 同一阶段内按 parallelSort、节点 ID 排序，位次从 1 开始。
 * 同名节点跨多个阶段时，serialStep 是该名称出现的阶段位次；只出现在一个阶段时为 null，
 * 单槽因此也不会被补成「第 1 步」。
 */
public final class SlotLabels {

    private SlotLabels() {
    }

    public record Position(int slotIndex, int slotTotal, Integer serialStep) {
    }

    public static Position of(Long nodeId, List<ItemStageNodeStatusBO> nodes) {
        if (nodeId == null || nodes == null || nodes.isEmpty()) {
            return new Position(1, 1, null);
        }
        ItemStageNodeStatusBO self = nodes.stream()
                .filter(node -> nodeId.equals(node.getNodeId()))
                .findFirst()
                .orElse(null);
        if (self == null || self.getSort() == null) {
            return new Position(1, 1, null);
        }
        List<ItemStageNodeStatusBO> stage = nodes.stream()
                .filter(node -> self.getSort().equals(node.getSort()))
                .sorted(Comparator
                        .comparing(ItemStageNodeStatusBO::getParallelSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ItemStageNodeStatusBO::getNodeId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        int slotIndex = 1;
        for (int i = 0; i < stage.size(); i++) {
            if (nodeId.equals(stage.get(i).getNodeId())) {
                slotIndex = i + 1;
                break;
            }
        }
        String name = self.getNodeName();
        List<Integer> sorts = nodes.stream()
                .filter(node -> Objects.equals(name, node.getNodeName()) && node.getSort() != null)
                .map(ItemStageNodeStatusBO::getSort)
                .distinct()
                .sorted()
                .toList();
        Integer serialStep = sorts.size() > 1 ? sorts.indexOf(self.getSort()) + 1 : null;
        return new Position(slotIndex, Math.max(stage.size(), 1), serialStep);
    }
}
