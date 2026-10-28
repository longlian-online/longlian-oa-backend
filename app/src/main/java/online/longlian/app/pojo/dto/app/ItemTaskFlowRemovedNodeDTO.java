package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "要删除的项目工作流节点")
public class ItemTaskFlowRemovedNodeDTO {

    @NotNull(message = "节点不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "节点 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "任务修订号不能为空")
    @Schema(description = "打开编辑时看到的任务修订号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String revision;
}
