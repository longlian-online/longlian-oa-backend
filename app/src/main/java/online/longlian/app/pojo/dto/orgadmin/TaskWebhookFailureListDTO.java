package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "失败的任务 webhook 列表参数")
public class TaskWebhookFailureListDTO {
    @NotNull
    @Min(1)
    @Schema(description = "页码，从 1 开始", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pageNum;

    @NotNull
    @Min(1)
    @Max(100)
    @Schema(description = "每页条数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pageSize;
}
