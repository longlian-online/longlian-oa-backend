package online.longlian.app.pojo.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import online.longlian.generator.enumeration.Status;

@Data
@Schema(description = "修改状态请求参数")
public class ChangeStatusDTO {

    @NotNull(message = "目标状态不能为空")
    @Schema(description = "目标状态：ENABLED-启用，DISABLED-禁用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Status status;
}
