package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建工坊个人任务流模板请求参数")
public class WorkshopTaskTemplateCreateDTO {

    @NotBlank(message = "流程模板名称不能为空")
    @Size(max = 100, message = "流程模板名称不能超过 100 个字符")
    @Schema(description = "流程模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "流程模板说明不能超过 500 个字符")
    @Schema(description = "流程模板说明")
    private String description;

    @NotEmpty(message = "模流程模板节点不能为空")
    @Valid
    @Schema(description = "流程模板节点列表（至少包含一个节点）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WorkshopTaskTemplateNodeCreateDTO> nodes;
}
