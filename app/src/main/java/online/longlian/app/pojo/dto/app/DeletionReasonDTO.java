package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "删除申请")
public class DeletionReasonDTO {

    @NotBlank(message = "删除原因不能为空")
    @Size(max = 500, message = "删除原因不能超过 500 个字符")
    @Schema(description = "删除原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
