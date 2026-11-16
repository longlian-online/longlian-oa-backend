package online.longlian.app.pojo.bo.app.itemtaskflow;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ItemTaskFlowUpdateParamsBO {
    Long itemId;
    Long orgId;
    Long operatorId;
    List<TaskFlowNodeParamsBO> nodes;
    List<TaskFlowNodeDeleteParamsBO> removed;

    @Value
    @Builder
    public static class TaskFlowNodeParamsBO {
        Long id;
        Long baseTaskId;
        Integer sort;
        Integer parallelSort;
        String name;
        Long revision;
    }

    @Value
    @Builder
    public static class TaskFlowNodeDeleteParamsBO {
        Long id;
        Long revision;
    }
}
