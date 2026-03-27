package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建项目任务流请求参数")
public class ItemTaskFlowCreateDTO {

    @NotNull(message = "任务模板ID不能为空")
    @Schema(
        description = "任务模板ID；创建时会将模板当前节点结构快照到任务流节点，后续模板变更不影响该任务流",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long taskTemplateId;
}
