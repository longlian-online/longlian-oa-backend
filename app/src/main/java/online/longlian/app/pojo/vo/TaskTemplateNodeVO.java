package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "任务模板节点信息")
public class TaskTemplateNodeVO {

    @Schema(description = "模板节点ID")
    private Long id;

    @Schema(description = "关联原子任务ID")
    private Long baseTaskId;

    @Schema(description = "原子任务名称")
    private String baseTaskName;

    @Schema(description = "原子任务图标URL")
    private String baseTaskIconUrl;

    @Schema(description = "是否需要上传文件：0-否 1-是")
    private Byte needFile;

    @Schema(description = "是否必须上传：0-否 1-是")
    private Byte requiredFile;

    @Schema(
        description = "步骤顺序（相同 sort 值表示并行节点）"
    )
    private Integer sort;

    @Schema(description = "并行组内排序")
    private Integer parallelSort;
}
