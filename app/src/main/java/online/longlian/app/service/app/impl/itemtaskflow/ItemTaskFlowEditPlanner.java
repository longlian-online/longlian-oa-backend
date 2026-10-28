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
 * 把「客户端看到的修订号」和「希望保存的节点」收成一份可执行计划。
 * 修订号对不上时拒绝整次保存，避免用旧视图覆盖期间发生的接取或提交。
 */
final class ItemTaskFlowEditPlanner {

    static final String STALE = "任务流已变更，请刷新后重试";
    static final String LOCKED_DELETE = "已接取或已有提交的任务不能删除";
    static final String BASE_TASK_CHANGED = "不能更换已有任务的原子任务";

    private ItemTaskFlowEditPlanner() {
    }

    record CurrentNode(long id, long baseTaskId, String name, long instanceId, long revision,
                       TaskInstanceStatus status, Long assigneeId, int submissionCount) {
    }

    record DesiredNode(Long id, long baseTaskId, int sort, int parallelSort, String name, Long revision) {
    }

    record RemovedNode(long id, long revision) {
    }

    record Kept(long nodeId, long instanceId, long revision, int sort, int parallelSort, String name) {
    }

    record Deleted(long nodeId, long instanceId, long revision) {
    }

    record Created(long baseTaskId, int sort, int parallelSort, String name) {
    }

    record Plan(List<Kept> kept, List<Deleted> deleted, List<Created> created) {
    }

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

    private record Positioned(DesiredNode node, int sort, int parallelSort) {
    }

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
