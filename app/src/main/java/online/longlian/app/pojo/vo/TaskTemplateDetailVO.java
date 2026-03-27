package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.Status;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "任务模板详情（含节点列表，用于模板编辑页和流程图预览）")
public class TaskTemplateDetailVO {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板说明")
    private String description;

    @Schema(description = "状态：ENABLED-启用，DISABLED-禁用")
    private Status status;

    @Schema(description = "引用次数")
    private Integer refCount;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建人昵称")
    private String creatorNickname;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(
        description = "节点列表（按 sort 升序排列；sort 相同的节点为并行节点，前端据此渲染分支结构）"
    )
    private List<TaskTemplateNodeVO> nodes;
}
