package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.util.List;

/**
 * 开放面任务流节点。字段冻结：只增不删；不含图标与站内编辑标记。
 */
@Data
@Schema(description = "开放接口任务流节点")
public class OpenItemFlowNodeVO {

    @Schema(description = "原子任务ID")
    private Long baseTaskId;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "提交字段快照，自动化按此组装 values")
    private List<OpenSubmitFieldVO> submitFields;

    @Schema(description = "阶段顺序")
    private Integer sort;

    @Schema(description = "并行组内顺序")
    private Integer parallelSort;

    @Schema(description = "任务实例ID，未生成时为空")
    private Long taskInstanceId;

    @Schema(description = "任务状态；null 表示前序未完成、节点未解锁")
    private TaskInstanceStatus taskStatus;

    @Schema(description = "任务修订号，接取/放弃/提交操作的乐观锁")
    private String revision;
}
