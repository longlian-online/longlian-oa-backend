package online.longlian.app.pojo.bo.app;

import lombok.Data;

/**
 * 组织负载候选行：某成员在当前组织已接取、尚未提交的任务。
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
}
