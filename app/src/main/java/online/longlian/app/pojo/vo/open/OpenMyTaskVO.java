package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.time.LocalDateTime;

/**
 * 开放面我的待办条目。字段冻结：只增不删。
 */
@Data
@Schema(description = "开放接口我的待办")
public class OpenMyTaskVO {

    @Schema(description = "任务实例ID")
    private Long taskInstanceId;

    @Schema(description = "任务节点名")
    private String nodeName;

    @Schema(description = "所属项目ID")
    private Long itemId;

    @Schema(description = "所属项目标题")
    private String itemTitle;

    @Schema(description = "所属企划ID")
    private Long projectId;

    @Schema(description = "所属企划标题")
    private String projectName;

    @Schema(description = "任务状态，待办恒为 CLAIMED")
    private TaskInstanceStatus taskStatus;

    @Schema(description = "任务修订号，接取/放弃/提交操作的乐观锁")
    private Long revision;

    @Schema(description = "接取时间")
    private LocalDateTime claimedAt;

    @Schema(description = "分组：SUBMITTABLE-可提交，WAITING-等前置")
    private MyTaskBucket bucket;
}
