package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "保存项目工作流")
public class ItemTaskFlowUpdateDTO {

    @Valid
    @NotEmpty(message = "请至少保留一个任务")
    @Schema(description = "保存后的节点。已有节点带 id 和修订号，新节点不带 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ItemTaskFlowNodeUpdateDTO> nodes;

    @Valid
    @Schema(description = "从当前工作流中删除的节点。必须带上打开编辑时看到的修订号")
    private List<ItemTaskFlowRemovedNodeDTO> removed;
}
