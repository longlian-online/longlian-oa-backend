package online.longlian.app.pojo.dto.app;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建项目请求参数")
public class ProjectItemCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "项目标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotNull(message = "流程模板不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "流程模板ID（任务模板ID）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskTemplateId;

    @Valid
    @Size(min = 1, message = "请至少添加一个任务节点")
    @Schema(description = "创建时调整后的步骤。不传则按模板原样生成，且全部待接取")
    private List<ProjectItemNodeCreateDTO> nodes;
}
