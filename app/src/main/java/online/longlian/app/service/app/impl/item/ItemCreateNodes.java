package online.longlian.app.service.app.impl.item;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.app.ItemNodeCreateParamsBO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把创建项目时提交的步骤收成连续阶段。
 * 编号规则与项目工作流保存相同：先按阶段和组内顺序排，再压掉空档。
 */
final class ItemCreateNodes {

    private ItemCreateNodes() {
    }

    record Node(long baseTaskId, int sort, int parallelSort, String name, Long assigneeId) {
    }

    static List<Node> normalize(List<ItemNodeCreateParamsBO> input) {
        if (input == null || input.isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, "请至少添加一个任务节点");
        }
        List<ItemNodeCreateParamsBO> ordered = new ArrayList<>(input);
        ordered.sort(Comparator.comparingInt(ItemNodeCreateParamsBO::getSort)
                .thenComparingInt(ItemNodeCreateParamsBO::getParallelSort));
        Map<Integer, Integer> sortMap = new LinkedHashMap<>();
        for (ItemNodeCreateParamsBO node : ordered) {
            if (node.getSort() == null || node.getParallelSort() == null
                    || node.getBaseTaskId() == null
                    || node.getSort() < 1 || node.getParallelSort() < 1) {
                throw new AppException(ResultCode.PARAM_ERROR, "流程位置无效");
            }
            sortMap.computeIfAbsent(node.getSort(), ignored -> sortMap.size() + 1);
        }
        Map<Integer, Integer> parallelCursor = new LinkedHashMap<>();
        List<Node> normalized = new ArrayList<>();
        for (ItemNodeCreateParamsBO node : ordered) {
            int sort = sortMap.get(node.getSort());
            int parallelSort = parallelCursor.merge(sort, 1, Integer::sum);
            normalized.add(new Node(node.getBaseTaskId(), sort, parallelSort,
                    blankToNull(node.getName()), node.getAssigneeId()));
        }
        return normalized;
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
}
