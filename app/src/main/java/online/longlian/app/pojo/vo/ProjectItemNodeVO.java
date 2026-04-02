package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.ItemNodeState;

@Data
@Schema(description = "项目流程节点")
public class ProjectItemNodeVO {

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点顺序（相同值表示并行组）")
    private Integer sort;

    @Schema(description = "并行组内排序")
    private Integer parallelSort;

    @Schema(description = "节点状态")
    private ItemNodeState state;

    @Schema(description = "并行子任务数量")
    private Integer parallelCount;
}
