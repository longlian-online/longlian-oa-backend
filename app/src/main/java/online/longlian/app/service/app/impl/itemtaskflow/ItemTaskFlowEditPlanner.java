package online.longlian.app.service.app.impl.itemtaskflow;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 纯内存地把一次保存收成可执行计划，不访问数据库。
 * <p>
 * 输入三份名单：库里的当前节点、希望留下的节点、明确删除的节点。
 * 当前节点必须被「留下」或「删除」恰好覆盖一次；已有节点的修订号必须和任务实例一致。
 * 对不上就拒绝整次保存，避免用旧视图覆盖期间发生的接取或提交。
 * <p>
 * 顺序会先按步骤、再按并行序号排好，再压成从 1 开始的连续编号，客户端传来的空档不影响结果。
 */
final class ItemTaskFlowEditPlanner {

    static final String STALE = "任务流已变更，请刷新后重试";
    static final String LOCKED_DELETE = "已接取或已有提交的任务不能删除";
    static final String BASE_TASK_CHANGED = "不能更换已有任务的原子任务";

    private ItemTaskFlowEditPlanner() {
    }

    /** 库里的节点，带上任务实例的修订号、状态和提交次数。 */
    record CurrentNode(long id, long baseTaskId, String name, long instanceId, long revision,
                       TaskInstanceStatus status, Long assigneeId, int submissionCount) {
    }

    /** 客户端希望保存后仍在的节点。没有 id 表示新增。 */
    record DesiredNode(Long id, long baseTaskId, int sort, int parallelSort, String name, Long revision) {
    }

    /** 客户端明确要删掉的节点，必须带打开编辑时看到的修订号。 */
    record RemovedNode(long id, long revision) {
    }

    /** 保留：只更新名称和顺序，并让任务实例修订号加一。 */
    record Kept(long nodeId, long instanceId, long revision, int sort, int parallelSort, String name) {
    }

    /** 删除：节点和任务实例一起软删。 */
    record Deleted(long nodeId, long instanceId, long revision) {
    }

    /** 新增：此时还没有节点和任务实例。 */
    record Created(long baseTaskId, int sort, int parallelSort, String name) {
    }

    /** 一次保存要执行的三类变更。 */
    record Plan(List<Kept> kept, List<Deleted> deleted, List<Created> created) {
    }

    /** 校验名单完整且修订号一致，再拆成保留、删除、新增。 */
    static Plan plan(List<CurrentNode> current, List<DesiredNode> desired, List<RemovedNode> removed) {
        requireDesired(desired);
        List<RemovedNode> removals = removed == null ? List.of() : removed;
        Map<Long, CurrentNode> currentById = indexCurrent(current);
        requireSameNodes(currentById.keySet(), desiredIds(desired), removedIds(removals));
        List<Kept> kept = new ArrayList<>();
        List<Created> created = new ArrayList<>();
        for (Positioned item : normalize(desired)) {
            collect(item, currentById, kept, created);
        }
        return new Plan(kept, deletions(removals, currentById), created);
    }

    private static void requireDesired(List<DesiredNode> desired) {
        if (desired == null || desired.isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, "请至少保留一个任务");
        }
    }

    private static Map<Long, CurrentNode> indexCurrent(List<CurrentNode> current) {
        Map<Long, CurrentNode> currentById = new LinkedHashMap<>();
        for (CurrentNode node : current) {
            if (currentById.put(node.id(), node) != null) {
                throw new AppException(ResultCode.OPERATION_FAIL, STALE);
            }
        }
        return currentById;
    }

    private static Set<Long> desiredIds(List<DesiredNode> desired) {
        Set<Long> ids = new HashSet<>();
        for (DesiredNode node : desired) {
            if (node.id() != null && !ids.add(node.id())) {
                throw new AppException(ResultCode.PARAM_ERROR, "节点重复");
            }
        }
        return ids;
    }

    private static Set<Long> removedIds(List<RemovedNode> removals) {
        Set<Long> ids = new HashSet<>();
        for (RemovedNode node : removals) {
            if (!ids.add(node.id())) {
                throw new AppException(ResultCode.PARAM_ERROR, "节点重复");
            }
        }
        return ids;
    }

    /**
     * 当前节点必须被「留下」或「删除」正好覆盖一遍。
     * 同一节点两边都出现是参数错误；漏掉或带上库里没有的节点，说明视图已过期。
     */
    private static void requireSameNodes(Set<Long> currentIds, Set<Long> desiredIds, Set<Long> removedIds) {
        if (overlaps(desiredIds, removedIds) || !currentIds.equals(union(desiredIds, removedIds))) {
            throw new AppException(desiredIdsRemoveOverlap(desiredIds, removedIds)
                    ? ResultCode.PARAM_ERROR : ResultCode.OPERATION_FAIL,
                    desiredIdsRemoveOverlap(desiredIds, removedIds) ? "节点重复" : STALE);
        }
    }

    private static boolean overlaps(Set<Long> left, Set<Long> right) {
        for (Long id : left) {
            if (right.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean desiredIdsRemoveOverlap(Set<Long> desiredIds, Set<Long> removedIds) {
        return overlaps(desiredIds, removedIds);
    }

    /** 没有 id 的是新增；有 id 的只能改名称和顺序，不能换原子任务。 */
    private static void collect(Positioned item, Map<Long, CurrentNode> currentById,
                                List<Kept> kept, List<Created> created) {
        DesiredNode node = item.node();
        if (node.id() == null) {
            created.add(new Created(node.baseTaskId(), item.sort(), item.parallelSort(), blankToNull(node.name())));
            return;
        }
        kept.add(keep(item, requireCurrent(currentById, node.id(), node.revision()), node));
    }

    private static Kept keep(Positioned item, CurrentNode existing, DesiredNode node) {
        if (node.baseTaskId() != existing.baseTaskId()) {
            throw new AppException(ResultCode.PARAM_ERROR, BASE_TASK_CHANGED);
        }
        String name = blankToNull(node.name());
        return new Kept(existing.id(), existing.instanceId(), existing.revision(),
                item.sort(), item.parallelSort(), name == null ? existing.name() : name);
    }

    /** 只有仍待接取、没有接取人、也没有任何提交记录的任务才能删。 */
    private static List<Deleted> deletions(List<RemovedNode> removals, Map<Long, CurrentNode> currentById) {
        List<Deleted> deleted = new ArrayList<>();
        for (RemovedNode node : removals) {
            CurrentNode existing = requireCurrent(currentById, node.id(), node.revision());
            if (!canDelete(existing)) {
                throw new AppException(ResultCode.OPERATION_FAIL, LOCKED_DELETE);
            }
            deleted.add(new Deleted(existing.id(), existing.instanceId(), existing.revision()));
        }
        return deleted;
    }

    /** 修订号是乐观锁。缺失、找不到，或和库里不一致，都视为任务流已变更。 */
    private static CurrentNode requireCurrent(Map<Long, CurrentNode> currentById, long id, Long revision) {
        CurrentNode existing = currentById.get(id);
        if (existing == null || revision == null || revision != existing.revision()) {
            throw new AppException(ResultCode.OPERATION_FAIL, STALE);
        }
        return existing;
    }

    private static boolean canDelete(CurrentNode node) {
        return node.status() == TaskInstanceStatus.PENDING
                && node.assigneeId() == null
                && node.submissionCount() == 0;
    }

    /** 空名称表示不改已有节点的原名；新增节点的空名称留给写入时用原子任务名。 */
    private static String blankToNull(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 100) {
            throw new AppException(ResultCode.PARAM_ERROR, "节点名称不能超过 100 个字符");
        }
        return trimmed;
    }

    private static Set<Long> union(Set<Long> left, Set<Long> right) {
        Set<Long> ids = new HashSet<>(left);
        ids.addAll(right);
        return ids;
    }

    /** 重编后的位置：sort 是连续步骤，parallelSort 是同一步骤内的排队序号。 */
    private record Positioned(DesiredNode node, int sort, int parallelSort) {
    }

    /**
     * 按客户端给出的步骤和并行序号排序，再重编成连续序号。
     * 相同步骤的节点并行，组内按原并行序号排队；中间缺号会被压掉。
     */
    private static List<Positioned> normalize(List<DesiredNode> desired) {
        List<DesiredNode> ordered = new ArrayList<>(desired);
        ordered.sort(Comparator.comparingInt(DesiredNode::sort)
                .thenComparingInt(DesiredNode::parallelSort));
        Map<Integer, Integer> sortMap = new LinkedHashMap<>();
        for (DesiredNode node : ordered) {
            if (node.sort() < 1 || node.parallelSort() < 1) {
                throw new AppException(ResultCode.PARAM_ERROR, "流程位置无效");
            }
            sortMap.computeIfAbsent(node.sort(), ignored -> sortMap.size() + 1);
        }
        Map<Integer, Integer> parallelCursor = new LinkedHashMap<>();
        List<Positioned> positioned = new ArrayList<>();
        for (DesiredNode node : ordered) {
            int sort = sortMap.get(node.sort());
            int parallelSort = parallelCursor.merge(sort, 1, Integer::sum);
            positioned.add(new Positioned(node, sort, parallelSort));
        }
        return positioned;
    }
}
