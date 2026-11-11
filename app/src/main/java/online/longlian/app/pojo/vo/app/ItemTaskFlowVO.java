package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.ItemStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目任务流信息（含节点执行状态，用于流程图可视化）")
public class ItemTaskFlowVO {

    @Schema(description = "项目名称")
    private String itemTitle;

    @Schema(description = "项目状态：IN_PROGRESS-进行中，COMPLETED-已完成")
    private ItemStatus status;

    @Schema(description = "标记完成时间，进行中时为空")
    private LocalDateTime completedAt;

    @Schema(description = "任务流名称")
    private String name;

    @Schema(description = "任务流说明")
    private String description;

    @Schema(description = "是否存在提交记录（含已打回、已重置），决定删除项目是直接删除还是提交删除申请")
    private Boolean hasSubmissionRecord;

    @Schema(
        description = "节点列表（按 sort 升序；sort 相同的节点为并行节点，前端根据此结构渲染流程图分支）"
    )
    private List<ItemTaskNodeVO> nodes;
}
