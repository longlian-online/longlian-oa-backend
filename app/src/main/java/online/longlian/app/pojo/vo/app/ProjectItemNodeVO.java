package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.ItemNodeState;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
