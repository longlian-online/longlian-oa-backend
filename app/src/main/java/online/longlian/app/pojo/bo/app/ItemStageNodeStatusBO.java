package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目内一个任务节点的完成情况（自定义 SQL 结果），供前置判定使用。
 * <p>
 * completed：节点存在实例且全部实例为 COMPLETED，与前端节点 taskStatus === "COMPLETED" 对齐。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemStageNodeStatusBO {
    private Long itemId;
    private Long nodeId;
    private String nodeName;
    private Integer sort;
    private Integer parallelSort;
    private Boolean completed;
}
