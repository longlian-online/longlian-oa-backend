package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ItemTaskFlowUpdateParamsBO {
    Long itemId;
    Long orgId;
    Long operatorId;
    List<Node> nodes;
    List<Removed> removed;

    @Value
    @Builder
    public static class Node {
        Long id;
        Long baseTaskId;
        Integer sort;
        Integer parallelSort;
        String name;
        Long revision;
    }

    @Value
    @Builder
    public static class Removed {
        Long id;
        Long revision;
    }
}
