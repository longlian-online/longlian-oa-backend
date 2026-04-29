package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建项目请求参数")
public class ProjectItemCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "项目标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotNull(message = "流程模板不能为空")
    @Schema(type = "string", description = "流程模板ID（任务模板ID）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskTemplateId;
}
