package online.longlian.app.pojo.bo.app;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的待办候选行（自定义 SQL 结果）：当前用户已接取（CLAIMED）的任务实例。
 */
@Data
public class MyTaskRowBO {
    private Long instanceId;
    private Long itemId;
    private Long projectId;
    private String itemTitle;
    private String projectName;
    private String nodeName;
    private Long nodeId;
    private Integer stageSort;
    private LocalDateTime claimedAt;
}
