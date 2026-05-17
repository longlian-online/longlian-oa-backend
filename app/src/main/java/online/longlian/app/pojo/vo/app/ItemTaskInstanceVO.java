package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.time.LocalDateTime;

@Data
@Schema(description = "项目任务实例（用于任务列表视图）")
public class ItemTaskInstanceVO {

    @JsonLongIdString
    @Schema(type = "string", description = "任务实例ID")
    private Long id;

    @Schema(description = "任务节点名称")
    private String name;

    @Schema(description = "任务实例状态：PENDING-待接取，CLAIMED-待提交，COMPLETED-已完成")
    private TaskInstanceStatus status;

    @JsonLongIdString
    @Schema(type = "string", description = "接取人ID")
    private Long assigneeId;

    @Schema(description = "接取人昵称")
    private String assigneeNickname;

    @Schema(description = "接取人头像URL")
    private String assigneeAvatarUrl;

    @Schema(description = "步骤顺序号（sort，用于列表排序）")
    private Integer sort;

    @Schema(description = "并行组内排序号")
    private Integer parallelSort;

    @Schema(description = "实例创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "完成时间，null 表示未完成")
    private LocalDateTime completedAt;
}
