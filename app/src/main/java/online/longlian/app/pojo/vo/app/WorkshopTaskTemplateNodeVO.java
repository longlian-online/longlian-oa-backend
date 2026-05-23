package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "工坊任务流模板节点信息（卡片简化版，不含 metaSchema）")
public class WorkshopTaskTemplateNodeVO {

    @JsonLongIdString
    @Schema(type = "string", description = "原子任务ID")
    private Long baseTaskId;

    @Schema(description = "原子任务名称（如：创建/翻译/校对/审核/发布）")
    private String baseTaskName;

    @Schema(description = "原子任务图标URL")
    private String baseTaskIconUrl;

    @Schema(description = "自定义任务实例名（如\"文字校对1\"），未设置时为空，前端优先展示此名称")
    private String customName;

    @Schema(description = "步骤顺序（相同 sort 值表示并行节点，前端按 sort 分组渲染阶段）")
    private Integer sort;

    @Schema(description = "并行组内排序（sort 相同时，用于控制并行节点的展示顺序）")
    private Integer parallelSort;
}
