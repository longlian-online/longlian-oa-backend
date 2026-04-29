package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "项目任务流信息（含节点执行状态，用于流程图可视化）")
public class ItemTaskFlowVO {

    @Schema(type = "string", description = "任务流ID")
    private Long id;

    @Schema(type = "string", description = "所属项目ID")
    private Long itemId;

    @Schema(type = "string", description = "所属企划ID")
    private Long projectId;

    @Schema(type = "string", description = "关联任务模板ID")
    private Long taskTemplateId;

    @Schema(description = "任务流名称")
    private String name;

    @Schema(description = "任务流说明")
    private String description;

    @Schema(
        description = "节点列表（按 sort 升序；sort 相同的节点为并行节点，前端根据此结构渲染流程图分支）"
    )
    private List<ItemTaskNodeVO> nodes;
}
