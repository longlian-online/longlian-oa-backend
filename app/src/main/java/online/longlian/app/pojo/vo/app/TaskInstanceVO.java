package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.generator.enumeration.TaskInstanceStatus;

import java.time.LocalDateTime;

@Data
@Schema(description = "任务实例信息（企划详情页右侧可接取任务列表）")
public class TaskInstanceVO {

    @Schema(type = "string", description = "任务实例ID")
    private Long id;

    @Schema(type = "string", description = "所属项目ID")
    private Long itemId;

    @Schema(type = "string", description = "所属企划ID")
    private Long projectId;

    @Schema(type = "string", description = "项目任务节点ID")
    private Long itemTaskNodeId;

    @Schema(description = "任务类型名称（如：翻译、校对）")
    private String baseTaskName;

    @Schema(description = "任务说明")
    private String baseTaskDescription;

    @Schema(description = "任务实例状态")
    private TaskInstanceStatus status;

    @Schema(type = "string", description = "接取人ID（已接取时有值）")
    private Long assigneeId;

    @Schema(description = "接取人昵称")
    private String assigneeNickname;

    @Schema(description = "接取人头像URL")
    private String assigneeAvatarUrl;

    @Schema(description = "接取/分配时间")
    private LocalDateTime createdAt;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;
}
