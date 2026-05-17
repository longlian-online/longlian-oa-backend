package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import online.longlian.common.enumeration.TaskTemplateScope;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "工坊中的任务流模板信息（卡片展示用）")
public class WorkshopTaskTemplateVO {

    @JsonLongIdString
    @Schema(type = "string", description = "模板ID")
    private Long id;

    @Schema(description = "流程模板名称")
    private String name;

    @Schema(description = "模板说明")
    private String description;

    @Schema(description = "模板所属范围：PERSONAL-个人模板，ORGANIZATION-组织通用模板")
    private TaskTemplateScope scope;

    @Schema(description = "任务总数（所有 task_template_node 记录数）")
    private Integer taskCount;

    @Schema(description = "节点列表（按 sort 升序排列；sort 相同的节点为并行节点，前端据此渲染分组和并行标识如 +N）")
    private List<WorkshopTaskTemplateNodeVO> nodes;

    @Schema(description = "是否为我创建的模板")
    private Boolean isMine;

}
