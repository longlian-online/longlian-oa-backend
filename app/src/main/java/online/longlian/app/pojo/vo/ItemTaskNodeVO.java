package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "任务流节点信息（含执行状态，用于流程图可视化）")
public class ItemTaskNodeVO {

    @Schema(description = "任务节点ID")
    private Long id;

    @Schema(description = "关联原子任务ID")
    private Long baseTaskId;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "步骤顺序（相同 sort 值为并行节点）")
    private Integer sort;

    @Schema(description = "并行组内排序")
    private Integer parallelSort;

    @Schema(description = "是否需要上传文件：0-否 1-是")
    private Byte needFile;

    @Schema(description = "是否必须上传：0-否 1-是")
    private Byte requiredFile;

    @Schema(
        description = "任务实例状态：1-PENDING(待接取) 2-CLAIMED(已接取) 3-COMPLETED(已完成)，null 表示该节点尚未生成实例（前序节点未完成）"
    )
    private Integer taskStatus;

    @Schema(description = "接取人ID（已接取时有值）")
    private Long assigneeId;

    @Schema(description = "接取人昵称")
    private String assigneeNickname;

    @Schema(description = "接取人头像URL")
    private String assigneeAvatarUrl;
}
