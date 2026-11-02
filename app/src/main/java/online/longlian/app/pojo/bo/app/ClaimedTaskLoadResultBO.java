package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.enumeration.MyTaskBucket;

import java.time.LocalDateTime;

/**
 * 组员名下已接取任务：{@link MyTaskBucket} 与「我的待办」同口径，
 * 供组织管理端展示任务数并区分进行中与等待中。
 */
@Data
@Builder
public class ClaimedTaskLoadResultBO {
    private Long assigneeId;
    private Long instanceId;
    private Long itemId;
    private Long projectId;
    private String itemTitle;
    private String projectName;
    private String nodeName;
    private Integer slotIndex;
    private Integer slotTotal;
    private Integer serialStep;
    private LocalDateTime claimedAt;
    private MyTaskBucket bucket;
    private String waitingStageName;
    private Integer waitingSlotIndex;
    private Integer waitingSlotTotal;
    private Integer waitingSerialStep;
}
