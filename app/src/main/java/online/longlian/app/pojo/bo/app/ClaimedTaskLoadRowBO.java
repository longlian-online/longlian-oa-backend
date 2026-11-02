package online.longlian.app.pojo.bo.app;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组员负载候选行（自定义 SQL 结果）：指定成员在当前组织内已接取（CLAIMED）、尚未提交的任务。
 */
@Data
public class ClaimedTaskLoadRowBO {
    private Long instanceId;
    private Long assigneeId;
    private Long itemId;
    private Long projectId;
    private String itemTitle;
    private String projectName;
    private String nodeName;
    private Long nodeId;
    private Integer stageSort;
    private LocalDateTime claimedAt;
}
