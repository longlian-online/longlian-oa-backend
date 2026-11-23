package online.longlian.app.pojo.dto.open;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
@Schema(description = "开放接口放弃任务请求")
public class OpenTaskAbandonDTO {

    @NotNull(message = "任务修订号不能为空")
    @PositiveOrZero(message = "任务修订号必须为非负整数")
    @Schema(description = "接取后任务当前修订号 revision，旧修订号不会执行操作", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long expectedRevision;
}
